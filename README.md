# Bond Price Calculator

A Java fixed-income analytics engine for pricing fixed-rate coupon bonds and computing sensitivity measures. Built as a quantitative finance exercise demonstrating real-world bond mathematics, OOP design, and concurrent batch processing.

---

## Table of Contents

- [Overview](#overview)
- [Project Structure](#project-structure)
- [Core Modules](#core-modules)
  - [Bond (securities)](#bond)
  - [FixedRateBondPricer](#fixedratebondpricer)
  - [Sensitivity Analysis](#sensitivity-analysis)
  - [Day-Count Conventions](#day-count-conventions)
  - [BondCSVParser](#bondcsvparser)
  - [BondApplication](#bondapplication)
- [gRPC Microservices Layer](#grpc-microservices-layer)
  - [Proto Schema](#proto-schema)
  - [PricingService](#pricingservice)
  - [SensitivityService](#sensitivityservice)
  - [Server](#server)
  - [Client](#client)
  - [Running the gRPC System](#running-the-grpc-system)
- [Financial Background](#financial-background)
  - [Present Value & Dirty Price](#present-value--dirty-price)
  - [Accrued Interest & Clean Price](#accrued-interest--clean-price)
  - [Yield-to-Maturity (Newton-Raphson)](#yield-to-maturity-newton-raphson)
  - [Macaulay Duration](#macaulay-duration)
  - [Modified Duration](#modified-duration)
  - [DV01](#dv01)
- [CSV Input Format](#csv-input-format)
- [Running the Application](#running-the-application)
- [JVM Configuration — ZGC](#jvm-configuration--zgc)

---

## Overview

The application reads bond data from a CSV file, prices each bond concurrently using a thread pool, and prints a summary of pricing and risk metrics per bond. All pricing is performed by discounting cash flows from the settlement date using a recomputed yield-to-maturity solved via Newton-Raphson iteration with an analytic derivative.

**Java version:** 25
**Build tool:** Maven

---

## Project Structure

```
src/main/java/com/fixedIncome/
├── BondApplication.java                    # Entry point — batch pricing with ExecutorService
├── securities/
│   └── Bond.java                           # Immutable bond model
├── pricer/
│   └── FixedRateBondPricer.java            # Dirty/clean price, accrued interest, YTM solver
├── sensitivityanalysis/
│   ├── BondMacaulayDuration.java           # Macaulay duration
│   ├── BondModifiedDuration.java           # Modified duration
│   └── BondDV01.java                       # DV01 (dollar value of a basis point)
├── daycount/
│   ├── DayCountConvention.java             # Interface
│   ├── DayCountFactory.java                # Factory / registry
│   ├── Act360.java                         # ACT/360
│   ├── Act365.java                         # ACT/365
│   ├── ActActISDA.java                     # ACT/ACT ISDA
│   ├── ActActICMA.java                     # ACT/ACT ICMA (frequency-aware)
│   └── Thirty360US.java                    # 30/360 US
├── parser/
│   └── BondCSVParser.java                  # CSV → List<Bond>
└── grpc/
    ├── GrpcServer.java                     # gRPC server entry point (port 9090)
    ├── GrpcBondClient.java                 # gRPC client — mirrors BondApplication over the wire
    ├── BondMapper.java                     # Bond ↔ BondMessage conversion
    ├── PricingServiceImpl.java             # PricingService implementation
    └── SensitivityServiceImpl.java         # SensitivityService implementation

src/main/proto/
└── bond.proto                              # Protobuf schema — messages + service definitions
```

---

## Core Modules

### Bond

`com.fixedIncome.securities.Bond`

An immutable value object representing a fixed-rate coupon bond. All fields are validated at construction; an `IllegalArgumentException` is thrown for any invalid input, guaranteeing that every constructed `Bond` instance is valid.

| Field | Type | Description |
|-------|------|-------------|
| `issuer` | `String` | Issuer name (required, non-empty) |
| `ISIN` | `String` | International Securities Identification Number — validated against the regex `[A-Z]{2}[A-Z0-9]{9}[0-9]` |
| `faceValue` | `double` | Par value (must be > 0) |
| `couponRate` | `double` | Annual coupon rate as a decimal, e.g. `0.05` for 5% (must be > 0) |
| `paymentFrequency` | `int` | Coupon payments per year — `1` annual, `2` semi-annual, `4` quarterly |
| `issueDate` | `LocalDate` | Issue date |
| `maturityDate` | `LocalDate` | Maturity date (must be after `issueDate`) |
| `dayCountConvention` | `String` | Convention name, e.g. `"ACT/365"` — validated eagerly via `DayCountFactory` |
| `spRating` | `String` | S&P credit rating, e.g. `"AA+"` |
| `settlementDate` | `LocalDate` | Trade settlement date |
| `cleanPrice` | `double` | Market-quoted clean price |
| `dirtyPrice` | `double` | Full (dirty) price including accrued interest |
| `quotedYield` | `double` | Vendor-quoted annual yield — used as the Newton-Raphson seed |

**Derived fields computed at construction:**

| Field | Description |
|-------|-------------|
| `couponPayment` | Periodic coupon = `faceValue × couponRate / paymentFrequency` |
| `couponPaymentDates` | Full coupon schedule from issue to maturity (unmodifiable `List<LocalDate>`) |
| `resolvedDayCountConvention` | Live `DayCountConvention` instance resolved from the convention string |

---

### FixedRateBondPricer

`com.fixedIncome.pricer.FixedRateBondPricer`

Provides four operations, all using fractional-period discounting so that settlement need not fall on a coupon date.

| Method | Description |
|--------|-------------|
| `dirtyPrice(bond, ytm)` | Sum of discounted future coupon payments plus discounted principal |
| `cleanPrice(bond, ytm)` | `dirtyPrice − accruedInterest` |
| `accruedInterest(bond)` | Interest accrued from the last coupon date to (but not including) the settlement date |
| `solveYTM(bond, targetDirtyPrice)` | Newton-Raphson solver — returns the YTM that exactly reproduces the given dirty price |

**YTM Solver details:**
- Seeded with `bond.getQuotedYield()` for fast convergence.
- Analytic derivative `dP/dy` is computed in the same pass as the price.
- Convergence threshold: `1e-10`; maximum iterations: `100`.
- Throws `ArithmeticException` if the derivative collapses or the estimate diverges outside `(−0.999, 100.0)`.

---

### Sensitivity Analysis

#### BondMacaulayDuration

PV-weighted average time (in years) to receive each cash flow. Uses the same fractional first-period offset `w` as the pricer for consistent discounting.

#### BondModifiedDuration

```
Modified Duration = Macaulay Duration / (1 + YTM / paymentFrequency)
```

Measures the percentage change in price for a 1% parallel shift in yield.

#### BondDV01

```
DV01 = Modified Duration × Dirty Price × 0.0001
```

Dollar change in bond price for a 1 basis point (0.01%) decrease in yield.

---

### Day-Count Conventions

`com.fixedIncome.daycount`

All conventions implement the `DayCountConvention` interface:

```java
double yearFraction(LocalDate start, LocalDate end);
String getName();
```

`DayCountFactory` resolves a convention by name (case-insensitive). Supported names:

| Name | Class | Notes |
|------|-------|-------|
| `ACT/360` | `Act360` | |
| `ACT/365` | `Act365` | |
| `30/360` / `30/360US` / `30/360 US` | `Thirty360US` | |
| `ACT/ACT ISDA` | `ActActISDA` | |
| `ACT/ACT ICMA` | `ActActICMA` | Frequency-aware; use `getConvention(name, frequency)` |

`DayCountFactory.getConvention(name, frequency)` returns a correctly configured `ActActICMA` instance for the bond's payment frequency; for all other conventions the `frequency` argument is ignored.

---

### BondCSVParser

`com.fixedIncome.parser.BondCSVParser`

Reads bond data from a comma-separated file. Blank lines and rows with fewer than the required number of fields are silently skipped. The header row is consumed and discarded.

**Expected column order:**

| Index | Field | Type |
|-------|-------|------|
| 0 | issuer | String |
| 1 | ISIN | String |
| 2 | faceValue | double |
| 3 | couponRate | double (decimal, e.g. `0.05`) |
| 4 | paymentFrequency | int |
| 5 | issueDate | `yyyy-MM-dd` |
| 6 | maturityDate | `yyyy-MM-dd` |
| 7 | dayCountConvention | String |
| 8 | spRating | String |
| 9 | settlementDate | `yyyy-MM-dd` |
| 10 | cleanPrice | double |
| 11 | dirtyPrice | double |
| 12 | quotedYield | double (decimal) |

---

### BondApplication

`com.fixedIncome.BondApplication`

Entry point for batch bond pricing. Reads `BondData.csv` from the working directory, submits one `Callable<BondResult>` per bond to a `FixedThreadPool` sized to `Runtime.getRuntime().availableProcessors()`, collects results via `invokeAll`, and prints the following per bond:

- Recomputed yield-to-maturity
- Recomputed clean price and dirty price
- Accrued interest
- Macaulay duration (years)
- Modified duration (years)
- DV01 (dollars per basis point)

Pricing errors for individual bonds are caught and printed to `stderr` without aborting the remaining results.

---

## Financial Background

### Present Value & Dirty Price

The dirty price is the sum of all future cash flows discounted from the settlement date. Fractional-period discounting handles settlement on non-coupon dates:

```
P_dirty = Σ [ CF_i / (1 + y/m)^(w + i) ]
```

Where:
- `CF_i` — cash flow at period `i` (coupon, or coupon + principal at maturity)
- `y` — annual yield-to-maturity
- `m` — payment frequency
- `w` — fraction of the current coupon period remaining as of settlement
- `i` — zero-based index over future coupon dates

### Accrued Interest & Clean Price

```
Accrued Interest = Coupon Payment × (days from last coupon to settlement) / (days in coupon period)
```

Day fractions are computed using the bond's day-count convention.

```
Clean Price = Dirty Price − Accrued Interest
```

### Yield-to-Maturity (Newton-Raphson)

The YTM `y` is the rate that satisfies `P_dirty(y) = targetDirtyPrice`. Newton-Raphson updates:

```
y_{n+1} = y_n − (P(y_n) − P_target) / (dP/dy)|_{y_n}
```

The analytic derivative of each discounted cash flow `CF / (1 + y/m)^(w+i)` with respect to `y` is:

```
d/dy = −(w + i) / m × CF / (1 + y/m)^(w+i+1)
```

### Macaulay Duration

```
D_mac = (1 / P_dirty) × Σ [ t_i × CF_i / (1 + y/m)^(w+i) ]
```

Where `t_i = (w + i) / m` is the time in years to cash flow `i`.

### Modified Duration

```
D_mod = D_mac / (1 + y/m)
```

### DV01

```
DV01 = D_mod × P_dirty × 0.0001
```

---

## CSV Input Format

```csv
issuer,ISIN,faceValue,couponRate,paymentFrequency,issueDate,maturityDate,dayCountConvention,spRating,settlementDate,cleanPrice,dirtyPrice,quotedYield
US Treasury,US912828ZT16,1000.0,0.025,2,2020-01-15,2030-01-15,ACT/ACT ICMA,AA+,2024-06-01,950.00,952.74,0.031
```

Dates must be ISO-8601 (`yyyy-MM-dd`). The coupon rate and quoted yield are decimals (e.g. `0.025` = 2.5%).

---

## Running the Application

1. Place `BondData.csv` in the project root (working directory from which the JVM is launched).
2. Build with Maven:
   ```bash
   mvn clean package
   ```
3. Run with the recommended JVM flags (see below):
   ```bash
   java [JVM flags] -cp target/OOP_Exercises-1.0-SNAPSHOT.jar com.fixedIncome.BondApplication
   ```

---

## JVM Configuration — ZGC

The application is configured to run with the Z Garbage Collector (ZGC), a low-latency, region-based GC that keeps pause times consistently below 1 ms regardless of heap size. The following flags are recommended:

```
-XX:+UseZGC
-Xms512m
-Xmx512m
-XX:SoftMaxHeapSize=400m
-XX:+AlwaysPreTouch
-Xlog:gc*:file=gc.log:time,uptime,level,tags:filecount=5,filesize=10m
-Xlog:safepoint=info
```

| Flag | Purpose |
|------|---------|
| `-XX:+UseZGC` | Enables the Z Garbage Collector |
| `-Xms512m` / `-Xmx512m` | Fixes heap size at 512 MB; eliminates heap-resize pauses |
| `-XX:SoftMaxHeapSize=400m` | Instructs ZGC to try to keep live data under 400 MB, leaving 112 MB of headroom for allocation spikes |
| `-XX:+AlwaysPreTouch` | Maps and zeroes all heap pages at JVM startup; eliminates OS page-fault latency at runtime |
| `-Xlog:gc*:file=gc.log:...` | Writes detailed GC events to a rolling log (`gc.log`) with a 5-file × 10 MB rotation policy |
| `-Xlog:safepoint=info` | Logs safepoint entry/exit to measure stop-the-world pauses |

---

## gRPC Microservices Layer

The pricing and sensitivity logic is exposed over **gRPC** with **Protocol Buffers** as the serialization layer. The existing domain classes are unchanged — the gRPC layer sits on top and delegates to them.

```
GrpcBondClient
      │  gRPC / TCP :9090
      ▼
GrpcServer
 ├── PricingServiceImpl     →  FixedRateBondPricer
 └── SensitivityServiceImpl →  BondMacaulayDuration / BondModifiedDuration / BondDV01
```

For full details see [`GRPC.md`](GRPC.md).

---

### Proto Schema

Defined in `src/main/proto/bond.proto`. Compiled to Java by `protobuf-maven-plugin` during `mvn generate-sources`.

**Shared message — `BondMessage`:** carries all bond static terms over the wire. Dates are ISO-8601 strings (`"YYYY-MM-DD"`).

---

### PricingService

| RPC | Type | Description |
|-----|------|-------------|
| `GetPrice` | Unary | Returns dirty price, clean price, and accrued interest at a given YTM |
| `SolveYTM` | Unary | Newton-Raphson solver — returns the YTM that reproduces the given dirty price |
| `GetPriceBatch` | Server-streaming | Streams one `PriceResponse` per bond in a batch |
| `SolveYTMBatch` | Server-streaming | Streams one `SolveYTMResponse` per bond in a batch |

---

### SensitivityService

| RPC | Type | Description |
|-----|------|-------------|
| `GetSensitivity` | Unary | Returns Macaulay duration, modified duration, and DV01 in a single response |
| `GetSensitivityBatch` | Server-streaming | Streams one `SensitivityResponse` per bond in a batch |

> All three sensitivity metrics are returned together because DV01 depends on modified duration, which depends on Macaulay duration.

---

### Server

`com.fixedIncome.grpc.GrpcServer` — starts both services on port `9090`. Registers a JVM shutdown hook for clean termination.

---

### Client

`com.fixedIncome.grpc.GrpcBondClient` — reads `BondData.csv` and replicates the `BondApplication` workflow over gRPC. For each bond it makes three sequential blocking RPCs:

1. `SolveYTM` — recompute yield from the market dirty price
2. `GetPrice` — dirty price, clean price, accrued interest at the solved YTM
3. `GetSensitivity` — Macaulay duration, modified duration, DV01

Output format is identical to `BondApplication`.

---

### Running the gRPC System

**Regenerate stubs after proto changes:**
```bash
mvn generate-sources
```

**Start the server:**
```
Run com.fixedIncome.grpc.GrpcServer
# Listening on localhost:9090
```

**Run the client** (requires `BondData.csv` in working directory):
```
Run com.fixedIncome.grpc.GrpcBondClient
```
