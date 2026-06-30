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
