# Bond Pricing Calculator (README IS CURRENTLY OUTDATED. YOU MAY REFER TO SOURCE CODE. IT IS WELL COMMENTED)

A Java exercise for practicing Object-Oriented Programming fundamentals in a quantitative finance context.

## Overview

This project models a fixed-rate bond and calculates its present value using discounted cash flow analysis. It demonstrates core OOP principles including encapsulation, immutability, and constructor validation.

## The Bond Class

### Attributes

| Field | Type | Description |
|-------|------|-------------|
| `faceValue` | `double` | Principal amount (par value) |
| `couponRate` | `double` | Annual coupon rate as decimal (e.g., 0.05 for 5%) |
| `yearsToMaturity` | `int` | Years until bond matures |
| `paymentFrequency` | `int` | Coupon payments per year (1=annual, 2=semi-annual, 4=quarterly) |

### Methods

| Method | Returns | Description |
|--------|---------|-------------|
| `getCouponPayment()` | `double` | Periodic coupon payment amount |
| `calculatePresentValue(double discountRate)` | `double` | Bond's present value at given annual yield |
| `toString()` | `String` | Human-readable bond summary |

## Financial Background

### Present Value Formula

The bond's present value is the sum of discounted coupon payments plus the discounted face value:

```
PV = C × [1 - (1 + r)^(-n)] / r + F / (1 + r)^n
```

Where:
- **C** = periodic coupon payment = (face value × annual coupon rate) / frequency
- **r** = periodic discount rate = annual discount rate / frequency
- **n** = total periods = years to maturity × frequency
- **F** = face value

### Example Calculation

A bond with:
- Face value: $1,000
- Coupon rate: 5% annually
- Maturity: 10 years
- Payment frequency: Semi-annual (2)
- Market yield: 6%

Periodic coupon: $1,000 × 0.05 / 2 = $25
Periodic rate: 0.06 / 2 = 0.03
Total periods: 10 × 2 = 20

Present value ≈ $925.61

## Usage

```java
Bond bond = new Bond(1000.0, 0.05, 10, 2);
double pv = bond.calculatePresentValue(0.06);

System.out.println(bond);
System.out.printf("Present Value at 6%% yield: %.2f%n", pv);
```

**Output:**
```
Bond[faceValue=1000.0, couponRate=5.0%, maturity=10yrs, frequency=semi-annual]
Present Value at 6% yield: 925.61
```

### Validation Strategy

Constructor validation follows the fail-fast principle:
1. Validate all inputs before any assignment
2. Throw `IllegalArgumentException` with descriptive messages
3. Guarantee that any constructed `Bond` instance is valid

### Periodic Rate Adjustment

The `calculatePresentValue` method accepts an annual discount rate and internally converts it to the periodic rate. This matches market convention where yields are quoted annually.

## Extension Ideas

- Add `calculateYieldToMaturity(double marketPrice)` using Newton-Raphson
- Implement `calculateDuration()` and `calculateConvexity()`
- Create a `BondPortfolio` class for aggregate risk metrics
- Add support for accrued interest and dirty/clean price distinction
