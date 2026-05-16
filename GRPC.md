# gRPC / Protobuf Layer

This document covers every file added to expose the bond pricing and sensitivity analysis engine over gRPC.

---

## Overview

The existing pricing and sensitivity logic (`com.fixedIncome.pricer`, `com.fixedIncome.sensitivityanalysis`) is unchanged. The gRPC layer sits on top of it, translating between Protobuf wire messages and the domain `Bond` object.

```
Client (GrpcBondClient)
        │
        │  gRPC over TCP :9090
        ▼
GrpcServer
 ├── PricingServiceImpl     →  FixedRateBondPricer
 └── SensitivityServiceImpl →  BondMacaulayDuration
                               BondModifiedDuration
                               BondDV01
```

---

## Proto Schema — `src/main/proto/bond.proto`

Defines all wire types and service contracts. The plugin compiles this into Java at build time (`mvn generate-sources`).

### Shared message: `BondMessage`

Carries all bond static terms over the wire. Mirrors the `Bond` domain object field-for-field. Dates are ISO-8601 strings (`"YYYY-MM-DD"`) because Protobuf has no native date type.

| Field | Type | Description |
|---|---|---|
| `issuer` | string | Issuer name |
| `isin` | string | 12-character ISIN |
| `face_value` | double | Par value |
| `coupon_rate` | double | Annual coupon rate as a decimal (e.g. `0.05` for 5%) |
| `payment_frequency` | int32 | Coupon payments per year (e.g. `2` = semi-annual) |
| `issue_date` | string | ISO-8601 date |
| `maturity_date` | string | ISO-8601 date |
| `day_count_convention` | string | e.g. `"ACT/365"`, `"30/360US"` |
| `sp_rating` | string | S&P credit rating |
| `settlement_date` | string | ISO-8601 date |
| `clean_price` | double | Market-quoted clean price |
| `dirty_price` | double | Market-quoted dirty price |
| `quoted_yield` | double | Vendor-quoted yield (used as Newton-Raphson seed) |

---

### `PricingService`

Four RPCs covering pricing and yield solving.

#### `GetPrice` — unary

Computes dirty price, clean price, and accrued interest at a given YTM.

```
PriceRequest  { BondMessage bond, double ytm }
PriceResponse { double dirty_price, double clean_price, double accrued_interest }
```

> `ytm` must be the recomputed yield from `SolveYTM`, not the vendor quoted yield.

#### `SolveYTM` — unary

Runs Newton-Raphson iteration to find the yield that reproduces the given dirty price. Seeds from `quoted_yield` on the bond. Throws `OUT_OF_RANGE` if the solver diverges.

```
SolveYTMRequest  { BondMessage bond, double target_dirty_price }
SolveYTMResponse { double ytm }
```

#### `GetPriceBatch` — server-streaming

Accepts a list of `PriceRequest` objects and streams back one `PriceResponse` per bond as each is computed.

```
BatchPriceRequest { repeated PriceRequest requests }
  → stream PriceResponse
```

#### `SolveYTMBatch` — server-streaming

Accepts a list of `SolveYTMRequest` objects and streams back one `SolveYTMResponse` per bond.

```
BatchSolveYTMRequest { repeated SolveYTMRequest requests }
  → stream SolveYTMResponse
```

---

### `SensitivityService`

Two RPCs returning all three risk metrics in a single response.

#### `GetSensitivity` — unary

Returns Macaulay duration, modified duration, and DV01 for a single bond at a given YTM.

```
SensitivityRequest  { BondMessage bond, double ytm }
SensitivityResponse { double macauley_duration, double modified_duration, double dv01 }
```

> All three metrics are returned together because DV01 depends on modified duration, which depends on Macaulay duration — computing them separately would be wasteful.

#### `GetSensitivityBatch` — server-streaming

Accepts a list of `SensitivityRequest` objects and streams back one `SensitivityResponse` per bond.

```
BatchSensitivityRequest { repeated SensitivityRequest requests }
  → stream SensitivityResponse
```

---

## Generated Code — `target/generated-sources/protobuf/`

Produced by `mvn generate-sources` via `protobuf-maven-plugin`. **Do not edit these files** — they are regenerated on every build.

| Path | Contents |
|---|---|
| `grpc-java/com/fixedIncome/grpc/PricingServiceGrpc.java` | Channel descriptors + `PricingServiceImplBase` to extend |
| `grpc-java/com/fixedIncome/grpc/SensitivityServiceGrpc.java` | Channel descriptors + `SensitivityServiceImplBase` to extend |
| `java/com/fixedIncome/grpc/BondMessage.java` | Protobuf message class |
| `java/com/fixedIncome/grpc/PriceRequest.java` | Protobuf message class |
| `java/com/fixedIncome/grpc/PriceResponse.java` | Protobuf message class |
| `java/com/fixedIncome/grpc/SolveYTMRequest.java` | Protobuf message class |
| `java/com/fixedIncome/grpc/SolveYTMResponse.java` | Protobuf message class |
| `java/com/fixedIncome/grpc/SensitivityRequest.java` | Protobuf message class |
| `java/com/fixedIncome/grpc/SensitivityResponse.java` | Protobuf message class |
| `java/com/fixedIncome/grpc/Batch*.java` | Batch/streaming request wrappers |

---

## Hand-written Source — `src/main/java/com/fixedIncome/grpc/`

### `BondMapper.java`

Utility class with two static methods bridging the proto/domain boundary.

| Method | Direction | Notes |
|---|---|---|
| `toProto(Bond)` | Domain → Wire | Serializes all fields; formats `LocalDate` as ISO-8601 string |
| `toDomain(BondMessage)` | Wire → Domain | Parses date strings with `LocalDate.parse()`; delegates to `Bond` constructor for validation |

---

### `PricingServiceImpl.java`

Extends `PricingServiceGrpc.PricingServiceImplBase`. Holds one `FixedRateBondPricer` instance and implements all four RPCs.

| Method | Delegates to | Error handling |
|---|---|---|
| `getPrice` | `pricer.dirtyPrice`, `pricer.accruedInterest` | `INVALID_ARGUMENT` on bad input |
| `solveYTM` | `pricer.solveYTM` | `OUT_OF_RANGE` if solver diverges; `INVALID_ARGUMENT` for bad bond data |
| `getPriceBatch` | same as `getPrice`, in a loop | `INTERNAL` on any mid-stream error |
| `solveYTMBatch` | same as `solveYTM`, in a loop | `INTERNAL` on any mid-stream error |

---

### `SensitivityServiceImpl.java`

Extends `SensitivityServiceGrpc.SensitivityServiceImplBase`. Holds instances of `FixedRateBondPricer`, `BondMacaulayDuration`, `BondModifiedDuration`, and `BondDV01`.

| Method | Delegates to | Notes |
|---|---|---|
| `getSensitivity` | All four sensitivity classes | Computes mac → mod → DV01 in order |
| `getSensitivityBatch` | Same, in a loop | Streams one `SensitivityResponse` per bond |

---

### `GrpcServer.java`

Entry point for the server process. Registers both services on port `9090` and blocks until shutdown. Installs a JVM shutdown hook for clean termination.

```
main() → ServerBuilder.forPort(9090)
             .addService(new PricingServiceImpl())
             .addService(new SensitivityServiceImpl())
             .build().start()
```

---

### `GrpcBondClient.java`

Entry point for the client process. Reads bonds from `BondData.csv`, then for each bond makes three sequential blocking RPCs and prints results in the same format as `BondApplication`.

**Per-bond RPC sequence:**
1. `PricingService/SolveYTM` — recompute yield from the market dirty price
2. `PricingService/GetPrice` — get dirty price, clean price, accrued interest at the solved YTM
3. `SensitivityService/GetSensitivity` — get Macaulay duration, modified duration, DV01

Per-bond `StatusRuntimeException` errors are printed to stderr and processing continues for remaining bonds. The channel is shut down in a `finally` block with a 5-second grace period.

---

## pom.xml Changes

| Change | Reason |
|---|---|
| Added `grpc-netty-shaded`, `grpc-protobuf`, `grpc-stub` dependencies | gRPC runtime |
| Added `protobuf-java` dependency | Protobuf runtime |
| Added `javax.annotation-api` dependency | Required by generated stub annotations |
| Added `os-maven-plugin` build extension | Detects OS/arch for native `protoc` binary download |
| Added `protobuf-maven-plugin` with `compile` + `compile-custom` goals | Generates Java message classes and gRPC service stubs from `.proto` |
| Added `<os.detected.classifier>windows-x86_64</os.detected.classifier>` property | Provides a default value so IntelliJ can resolve the property in static POM analysis (overridden at runtime by `os-maven-plugin`) |

---

## Running the System

**Start the server:**
```
Run GrpcServer.main()
# Listening on localhost:9090
```

**Run the client:**
```
Run GrpcBondClient.main()
# Requires BondData.csv in the working directory
```

**Regenerate stubs after proto changes:**
```bash
mvn generate-sources
```
