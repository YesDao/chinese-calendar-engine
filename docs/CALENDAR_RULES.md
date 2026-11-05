# Chinese Calendar Rules

This document describes the calendrical rules implemented by the Chinese Calendar Engine.

The library follows the traditional Chinese lunisolar calendar and uses astronomical calculations provided by the underlying calendar engine together with additional domain-specific logic.

---

# Day Boundary (子时)

Unlike the Gregorian calendar, the traditional Chinese calendar considers the beginning of the new calendrical day during **Zi Hour (子时)**.

In this project:

* Zi Hour begins at **23:00**.
* Dates after **23:00** belong to the next calendrical day.
* This rule is implemented by `ShanghaiTimeResolver.resolveZiDate()`.

Example:

| Local Time       | Calendrical Day |
| ---------------- | --------------- |
| 2024-05-20 22:59 | 2024-05-20      |
| 2024-05-20 23:00 | 2024-05-21      |
| 2024-05-21 00:30 | 2024-05-21      |

This convention is used for cache keys, Zi Hour calculations and all higher-level calendar computations.

---

# Year Boundary (立春)

The Chinese calendrical year does **not** begin on January 1st.

For GanZhi calculations the year changes at the exact astronomical moment of **Beginning of Spring (立春)**.

Example:

```
2024-02-04 16:25
Year Pillar = 癸卯

2024-02-04 16:26
Li Chun

2024-02-04 16:27
Year Pillar = 甲辰
```

This rule is used when calculating:

* Year Heavenly Stem
* Year Earthly Branch
* Four Pillars (BaZi)
* Tai Sui calculations

---

# Solar Term Ranges (节气)

Each Gregorian year contains exactly 24 Solar Terms, from Moderate Cold (小寒, early January) to Winter Solstice (冬至, late December).

A Solar Term has two kinds of boundaries:

* **Astronomical moment** — the exact time the Sun reaches the term's ecliptic longitude (`start`, `end`).
* **Zi days** — the calendrical days that belong to the term (`ziStart`, `ziEnd`, inclusive).

Zi days follow the Day Boundary rule above. The Zi day on which the next term begins already belongs to the next term. If a term begins after 23:00, its first Zi day is the following date.

Illustrative example — a term beginning at 23:30:

| Local Time       | Zi Day     | Active Term   |
| ---------------- | ---------- | ------------- |
| 2024-05-20 22:59 | 2024-05-20 | previous term |
| 2024-05-20 23:10 | 2024-05-21 | new term      |
| 2024-05-20 23:30 | 2024-05-21 | new term      |

The active term at a given moment is decided by its Zi day (`SolarTermRangeCache.resolveActiveSolarTerm`), not by the exact astronomical moment. This differs from the Year Boundary, which uses the exact moment of 立春.

Days in early January before Moderate Cold (小寒) belong to Winter Solstice (冬至) of the previous year.

## Absolute Index

Every term has an absolute index:

```
absoluteIndex = year * 24 + position   (position 0 = 小寒, 23 = 冬至)
```

The index is continuous across years, so the term before Moderate Cold (小寒) of a year is `index - 1`, which is Winter Solstice (冬至) of the previous year.

---

# Leap Months (闰月)

The Chinese lunisolar calendar occasionally inserts an additional lunar month.

The library follows the traditional representation:

* positive month → regular month
* negative month → leap month

Examples:

| Value | Meaning                       |
| ----: | ----------------------------- |
|     2 | Second lunar month            |
|    -2 | Leap second lunar month (闰二月) |
|     4 | Fourth lunar month            |
|    -4 | Leap fourth lunar month (闰四月) |

Convenience extension:

```kotlin
lunarDate.lunarMonthBase()
```

returns the canonical month number (1–12) regardless of leap status.

---

# Earth Transition Periods (土旺十八日)

Traditional Chinese cosmology considers four Earth transition periods during the year.

Each period begins **18 days before** one of the seasonal Solar Terms:

* Beginning of Spring (立春)
* Beginning of Summer (立夏)
* Beginning of Autumn (立秋)
* Beginning of Winter (立冬)

The library precomputes these periods and stores them in a thread-safe cache.

These periods are used to determine the active Earth Qi.

---

# Forecast Month

Forecast calculations use a modified month pillar.

Rules:

Regular month:

```
Forecast Month Pillar
=
Current Month Pillar
```

Leap month:

```
Forecast Month Pillar
=
Previous Month Pillar
```

This rule follows traditional Chinese metaphysical practice where leap months inherit the energetic influence of the preceding month.

The library exposes:

* monthStemForForecast
* monthBranchForForecast

separately from the ordinary month pillar.

---

# Timezone

All calculations are normalized to

```
Asia/Shanghai
```

which serves as the reference timezone for:

* Solar Terms
* GanZhi
* Lunar calendar
* Zi Hour
* Li Chun

Input values may originate from any timezone, but are converted to Shanghai time before calendrical calculations begin.

The current time is obtained with `ShanghaiTimeResolver.now(clock)` and `today(clock)`. The `Clock` parameter defaults to the system clock and can be replaced with a fixed clock in tests.
---

# Birth Time (真太阳时)

Civil time is not the Sun's time. All of China runs on UTC+8, and birth
times abroad are recorded in local civil zones. For a birth chart the
hour pillar must follow the Sun at the place of birth.

`LunarDateConverter.fromBirth(birth, longitude, mode, hourBranchShift)`:

* **Year and month pillars**, Solar Term, forecast and Tai Sui months and
  season come from the **real instant** of birth (same as `fromGregorian`).
  Solar Terms, including Li Chun, are astronomical moments.
* **Day and hour pillars** come from the birth time on the clock chosen by
  `BirthTimeMode`:
  * `BEIJING` — UTC+8, identical to `fromGregorian`;
  * `LOCAL_MEAN_SOLAR` — UTC + longitude × 4 min (平太阳时);
  * `TRUE_SOLAR` (default) — mean solar time + Equation of Time (真太阳时).
* The Zi Hour rule applies on that clock: from **23:00** the day pillar is
  the next day's.
* The hour stem is derived from the resolved day stem (五鼠遁), so a Zi hour
  after 23:00 uses the next day's stem.
* `hourBranchShift` moves the effective time by whole double hours (2 h)
  for manual calibration; crossing 23:00 moves the day pillar too.
* Clock fields (`lunarMonth`, `lunarDay`, `lunarHour`, `isLeapMonth`,
  `solarMonth`, `solarDay`, `solarHour`, `solarMinute`) follow the same
  chosen clock, including the calibration shift.

Example (Shanghai, 121.47° E):

| Beijing time     | True solar time | Day pillar of | Hour |
| ---------------- | --------------- | ------------- | ---- |
| 2026-11-03 22:45 | ≈ 23:07         | 2026-11-04    | 子   |

The Equation of Time uses the NOAA approximation (error < 30 s).
Implemented in `SolarTime`.
