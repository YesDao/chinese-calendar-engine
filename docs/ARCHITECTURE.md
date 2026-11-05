# Architecture

The Chinese Calendar Engine is organized into several independent layers.

```
Gregorian Date
        │
        ▼
ShanghaiTimeResolver
        │
        ▼
LunarDateConverter
        │
        ▼
LunarDate
        │
 ┌──────┼────────┐
 ▼      ▼        ▼
GanZhi SolarTerm FiveElements
```

---

# Layer Overview

## ShanghaiTimeResolver

Responsible for all timezone normalization.

Responsibilities:

* Convert any input timezone to Asia/Shanghai.
* Provide the current Shanghai time from an injectable `Clock` (`now(clock)`, `today(clock)`).
* Resolve Zi Hour date.
* Calculate Zi Hour index.
* Generate cache keys.

No calendrical logic is implemented here.

---

## LunarDateConverter

Main entry point of the library.

Responsibilities:

* Gregorian → Lunar conversion.
* Build strongly typed domain model.
* Resolve Four Pillars.
* Determine active Solar Term.
* Calculate forecast month.
* Calculate Tai Sui month.
* Resolve seasonal Qi.

Public API:

```kotlin
LunarDateConverter.fromGregorian(dateTime)
```

---

## LunarDate

Immutable domain model representing a complete Chinese calendrical snapshot.

Contains:

* Four Pillars (BaZi)
* Lunar month
* Lunar day
* Leap month information
* Solar Term
* Day index in the 60-cycle
* Forecast month pillars
* Tai Sui month pillars
* Season
* Five Element of seasonal Qi

---

# Domain Model

```
LunarDate
│
├── HeavenlyStem
├── EarthlyBranch
├── GanZhi
├── SolarTerm
├── FiveElement
├── FourSeason
├── YinYang
└── Organ
```

All domain concepts are represented as strongly typed Kotlin enums rather than raw strings.

---

# Cache Layer

The library contains three thread-safe caches.

## JieQiCache

Caches Solar Term tables by Gregorian year.

Purpose:

* avoid repeated astronomical calculations
* improve conversion performance

---

## SolarTermRangeCache

Built on top of `JieQiCache`. For each Gregorian year it holds the 24 Solar Terms as `SolarTermRange`:

* `start`, `end` — astronomical moments (end = start of the next term, exclusive)
* `ziStart`, `ziEnd` — Zi days of the term, inclusive
* `absoluteIndex` — `year * 24 + position`, continuous across years

Years around the current one (±10) are built at startup; any other year is built lazily on first access. Each year is built exactly once, also under concurrent access.

Incomplete source data (not exactly 24 terms, or no data for the following year) fails immediately instead of producing approximate ranges.

Public API:

```kotlin
SolarTermRangeCache.getForYear(year)
SolarTermRangeCache.getSolarTermRange(date)
SolarTermRangeCache.getSolarTermRange(year, term)
SolarTermRangeCache.resolveActiveSolarTerm(instant)
SolarTermRangeCache.getPreviousSolarTerm(year, term)
SolarTermRangeCache.findByAbsoluteIndex(index)
SolarTermRangeCache.findByAbsoluteIndexRange(fromIndex, toIndex)
```

---

## EarthTransitionCache

Caches Earth transition periods.

Each cache entry contains four intervals:

* before Beginning of Spring
* before Beginning of Summer
* before Beginning of Autumn
* before Beginning of Winter

---

# Testing Strategy

The project separates tests into several categories.

## LunarDateConverterTest

Verifies the public API.

Examples:

* Gregorian conversion
* timezone handling
* forecast month calculation

---

## ChineseCalendarInvariantTest

Verifies immutable calendrical rules.

Examples:

* Li Chun year transition
* GanZhi cycle
* Solar Terms
* Leap months
* New Moon boundaries
* JiaZi cycle

---

## ShanghaiTimeResolverTest

Verifies timezone normalization and Zi Hour calculations.

---

## Cache Tests

Infrastructure tests covering:

* JieQiCache
* SolarTermRangeCache — structure of each year, Zi-day rules, absolute index, lazy and concurrent building, and a check of all 24 moments of 2014 against Hong Kong Observatory data
* EarthTransitionCache

---

# Design Principles

The library follows several design principles.

* Immutable domain model.
* Strong typing instead of string values.
* Thread-safe caches.
* No hidden global clock: the current time comes from an injectable `Clock`.
* Clear separation between infrastructure and calendrical rules.
* Public API independent of the underlying astronomical implementation.