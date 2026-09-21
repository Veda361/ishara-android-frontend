# Ishaara Design System

**Document Version**: 1.0.0  
**Status**: ACTIVE / PRODUCTION DESIGN SYSTEM  
**Platform**: Android (Jetpack Compose & Material 3)  
**Target Roles**: User / Student & Driver / Conductor  

---

## 1. Brand Principles

The Ishaara visual design system is crafted around the concept of **Editorial Transportation Technology**:
- **Editorial Precision**: Generous negative space, high typographic hierarchy, clean horizontal dividers, and asymmetric layout balance.
- **Indian Urban Mobility**: Rooted in real transit environments — clean bus signage, route boards, and high-visibility status indicators.
- **Technical & Instrumentation**: Systematic brackets (`[ SYSTEM LIVE ]`, `[ ROUTE 01 ]`, `[ ETA 06 MIN ]`), monospace metadata, and telemetry readouts.
- **Minimalism & Confidence**: Crisp light and dark modes relying on thin borders, structured surfaces, and restrained accent usage rather than excessive gradients, glow, or drop shadows.
- **Youthful & Gen-Z**: Sharp editorial layout, responsive micro-interactions, and visual authenticity rather than childish cartoon iconography.

---

## 2. Color Tokens

The color system centers around a high-contrast monochrome foundation with a single restrained transit accent:

### Palette Structure
| Token | Light Value | Dark Value | Usage |
| :--- | :--- | :--- | :--- |
| `background` | `#FFFFFF` | `#0D0F12` | Screen canvas background |
| `surface` | `#F9FAFB` | `#14171D` | Primary card and container surfaces |
| `surfaceElevated` | `#FFFFFF` | `#1A1E26` | Elevated cards, dialogs, bottom sheets |
| `surfaceSubtle` | `#F3F4F6` | `#101318` | Inset blocks, badges, disabled fields |
| `foreground` | `#111827` | `#F9FAFB` | Primary headers and high-emphasis body |
| `foregroundMuted`| `#4B5563` | `#9CA3AF` | Supporting descriptions and secondary labels |
| `foregroundSubtle`| `#9CA3AF`| `#6B7280` | Placeholders, inactive icons, timestamps |
| `border` | `#D1D5DB` | `#262C38` | Standard component and card borders |
| `borderSubtle` | `#E5E7EB` | `#1C212B` | Hairline dividers and inset dividers |
| `borderStrong` | `#111827` | `#F9FAFB` | Focused inputs and high-contrast buttons |
| `accent` | `#F59E0B` | `#FBBF24` | Transit Amber / Marigold (CTAs, ETAs, active routes) |
| `accentForeground`| `#000000`| `#000000` | High contrast on accent surfaces |
| `success` | `#10B981` | `#10B981` | Online, active trip, confirmed boarding |
| `warning` | `#D97706` | `#FBBF24` | Approaching stop, pending request, low seats |
| `danger` | `#EF4444` | `#EF4444` | Emergency SOS, rejected, cancelled ride |
| `info` | `#3B82F6` | `#3B82F6` | Informational announcements and route hints |

---

## 3. Typography

The typography scale utilizes standard Android proportional fonts for editorial copy and system monospace for technical instrumentation:

```kotlin
// Technical instrumentation styles
IshaaraTheme.typography.technical       // 12sp Monospace, 1.0sp tracking (e.g. [ SYSTEM LIVE ])
IshaaraTheme.typography.technicalSmall  // 10sp Monospace, 1.2sp tracking (e.g. [ 01 ])

// Editorial headlines & titles
IshaaraTheme.typography.displayLarge    // 36sp Bold, -0.5sp tracking
IshaaraTheme.typography.displaySmall    // 28sp Bold, -0.25sp tracking
IshaaraTheme.typography.headlineLarge   // 24sp SemiBold
IshaaraTheme.typography.headlineMedium  // 20sp SemiBold
IshaaraTheme.typography.titleLarge      // 16sp SemiBold
IshaaraTheme.typography.bodyLarge       // 16sp Regular, 24sp line height
IshaaraTheme.typography.labelLarge      // 14sp SemiBold (Buttons)
```

---

## 4. Spacing

Spacing is governed by an 8-point structural grid with 4-point micro-adjustments:

| Token | Dimension | Intended Usage |
| :--- | :--- | :--- |
| `xxs` | `2.dp` | Hairline padding and dot indicator margins |
| `xs` | `4.dp` | Inline icon-to-text spacing, input label gaps |
| `sm` | `8.dp` | Inner badge padding, button icon gaps, chip spacing |
| `md` | `12.dp` | Standard card internal padding, list item vertical gaps |
| `lg` | `16.dp` | Screen edge horizontal margins, section padding |
| `xl` | `24.dp` | Distance between major screen sections |
| `xxl` | `32.dp` | Form group spacing, bottom sheet top clearance |
| `xxxl` | `48.dp` | Empty state vertical spacing, hero top padding |

---

## 5. Shapes

Ishaara maintains clean geometric surfaces with restrained corner rounding:
- `none`: `0.dp` (Full-width banners, dividers)
- `xs`: `4.dp` (Badges, technical tags, input fields)
- `sm`: `8.dp` (Standard cards, interactive buttons, modal containers)
- `md`: `12.dp` (Floating cards, interactive trip selectors)
- `lg`: `16.dp` (Bottom sheet top corners, dialog cards)
- `pill`: `999.dp` (Status dots, circular avatar clips)

---

## 6. Borders

Thin borders define Ishaara's clean architectural structure:
- `hairline`: `0.5.dp` (Dividers, technical tags)
- `thin`: `1.dp` (Standard card borders, text field borders)
- `default`: `1.dp` (Standard button outlines)
- `thick`: `2.dp` (Active focus rings, high-contrast selection)

---

## 7. Elevation

Elevation is used with extreme restraint; depth is achieved primarily through surface tone contrast and 1dp borders:
- `none`: `0.dp` (Standard cards, inline lists)
- `low`: `2.dp` (Floating search bar, active interactive cards)
- `medium`: `4.dp` (Sticky bottom bars, navigation bars)
- `high`: `8.dp` (Modal bottom sheets, emergency SOS dialogs)

---

## 8. Components

### 1. `IshaaraButton`
- **Variants**: `Primary` (high-contrast black/white), `Secondary` (subtle gray), `Outlined` (thin border), `Text` (minimal), `Danger` (crimson SOS/cancel).
- **Sizes**: `Small` (36dp), `Medium` (48dp), `Large` (56dp).
- **States**: Default, Pressed, Disabled, Loading (with inline circular progress).
- **Accessibility**: Minimum 48dp touch target enforced on medium/large buttons.

### 2. `IshaaraTextField`
- **Features**: Label, placeholder, helperText, errorText, leadingIcon, trailingIcon.
- **States**: Default, Focused, Error, Disabled, ReadOnly.
- **Visuals**: Thin border, high-contrast text, clear uppercase technical labels.

### 3. `IshaaraSearchField`
- **Features**: Dedicated transit discovery input with search glyph, clear button (`✕`), and dynamic loading spinner.

### 4. `IshaaraCard` & `IshaaraOutlinedCard`
- **Features**: Structured containers with 1dp border, zero drop shadow, and 8-12dp padding.

### 5. `IshaaraTechnicalLabel` & `IshaaraTechnicalValue`
- **Features**: Specialized instrument tags (`[ SYSTEM LIVE ]`, `[ ETA 06 MIN ]`) with monospace letter-spacing.

### 6. `IshaaraTopBar` & `IshaaraSectionHeader`
- **Features**: Editorial headers with system tag, back arrow, title, and section index numbers (`01 // ACTIVE TRIPS`).

### 7. `IshaaraBadge` & `IshaaraStatusBadge`
- **Features**: Compact status indicators with text and colored dot.

### 8. `IshaaraStatusIndicator`
- **Features**: Standalone accessible dot indicator + text representation.

### 9. `IshaaraIconButton`
- **Features**: Touch target 48dp min, clean container, accessible description.

### 10. `IshaaraDivider`
- **Features**: Hairline 0.5dp divider line.

### 11. `IshaaraSkeleton`
- **Features**: Pulsating placeholder with reduced motion support.

### 12. `IshaaraLoadingState`, `IshaaraEmptyState`, `IshaaraErrorState`, `IshaaraOfflineIndicator`
- **Features**: Complete suite of operational and recovery screens.

### 13. `IshaaraAvatar`
- **Features**: User/driver initials or photo avatar with status dot.

### 14. `IshaaraDialog` & `IshaaraBottomSheet`
- **Features**: Technical modal sheets and dialogs with thin borders.

---

## 9. Status System

Transit state is never communicated through color alone:

| Status | Dot Color | Label | Accessibility Content Description |
| :--- | :--- | :--- | :--- |
| `ONLINE` | Emerald Green | `ONLINE` | "Status: ONLINE" |
| `OFFLINE` | Slate Gray | `OFFLINE` | "Status: OFFLINE" |
| `ACTIVE` | Emerald Green | `ACTIVE` | "Status: ACTIVE" |
| `ARRIVING` | Transit Amber | `ARRIVING` | "Status: ARRIVING" |
| `IN_PROGRESS` | Transit Amber | `IN PROGRESS`| "Status: IN PROGRESS" |
| `COMPLETED` | Emerald Green | `COMPLETED` | "Status: COMPLETED" |
| `CANCELLED` | Crimson Red | `CANCELLED` | "Status: CANCELLED" |
| `REJECTED` | Crimson Red | `REJECTED` | "Status: REJECTED" |

---

## 10. Motion

Motion communicates state change, feedback, and navigation:
- `durationFast`: 150ms (Button press, checkbox toggle, dropdown reveal)
- `durationNormal`: 300ms (Card expansions, sheet dismissals, search reveal)
- `durationSlow`: 500ms (Major layout transitions)
- **Reduced Motion**: When system reduced motion is active, `resolveDuration()` collapses animation durations to `0ms` to avoid triggering motion sensitivity.

---

## 11. Accessibility (WCAG 2.1 AA)

- **Touch Targets**: All interactive elements (Buttons, IconButtons, Chips) have at least 48x48dp touch areas.
- **Color Independence**: Status badges include both text labels and dot indicators.
- **Contrast**: Text foreground colors meet at least 4.5:1 contrast against surface backgrounds.
- **Semantic Roles**: Proper Compose semantics (`Role.Button`, `contentDescription`) are attached to all actionable elements.

---

## 12. Iconography

- **Style**: Minimalist outline icons with 1.5dp to 2.0dp stroke weight.
- **Optical Sizes**: 16dp (badges), 20dp (buttons), 24dp (app bars).
- **Consistency**: All icons provide explicit `contentDescription` or are marked as decorative.

---

## 13. Asset Rules & Bus Visual Guidelines

1. **Photorealistic & Editorial**: Bus imagery in `assets/images/` and `res/drawable/` represents authentic Indian transit buses.
2. **Usage Context**: Bus visuals are reserved exclusively for brand hero moments, onboarding storytelling, empty states, and marketing banners.
3. **No Live GPS Replacement**: Static photographic assets MUST NOT be used as moving map pins on live GPS map tracking views. Live vehicles will use clean vector markers.
4. **No Baked Text**: Imagery does not bake English or regional text into graphic assets, preserving future localization.

---

## 14. Do / Don't Examples

### DO
- ✅ Use `IshaaraTechnicalLabel` for system status (`[ DRIVER ONLINE ]`, `[ ETA 12 MIN ]`).
- ✅ Use `IshaaraTheme.spacing` and `IshaaraTheme.colors` instead of raw `dp` or `Color(0x...)`.
- ✅ Prefer 1dp borders and subtle surface tone shifts over drop shadows.
- ✅ Combine status dots with explicit textual status labels.

### DON'T
- ❌ Do NOT use neon gradients or glowing cyberpunk cards.
- ❌ Do NOT make every button or card a full-rounded pill.
- ❌ Do NOT put business logic, API calls, or repositories inside UI components.
- ❌ Do NOT communicate status with only colored dots (e.g. green circle without text).
- ❌ Do NOT use cartoonish toy-bus graphics.
