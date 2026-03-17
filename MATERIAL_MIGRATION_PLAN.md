# Material UI Migration Plan

## Goal

Migrate the current Android XML/Data Binding UI toward a more consistent Material-based design system while preserving existing app behavior, view IDs, and UI test stability.

This plan assumes:

- the app remains XML/Data Binding based
- Jetpack Compose is out of scope for now
- migration happens incrementally, not as a rewrite
- the existing UI test suite must keep working or be updated in controlled steps

## Current baseline

### Theme and UI stack

- Base app theme: `app/src/main/res/values/styles.xml` → `AppTheme`
- Android 12+ theme override: `app/src/main/res/values-v31/themes.xml`
- Material library present: `app/build.gradle` → `com.google.android.material:material`
- UI is primarily XML + Data Binding
- Dialog UI already partially uses Material components in `app/src/main/res/layout/dialog_correct_answer.xml`

### Main gaps identified

- many layouts still use legacy widgets such as `Button`, `RadioButton`, `CheckBox`, and plain `TextView`
- visual styling relies heavily on custom drawables and hardcoded colors
- many layouts still use `Left` / `Right` constraints instead of `Start` / `End`
- spacing and typography are not consistently tokenized
- several surfaces are not theme-aware for DayNight
- custom selection controls are not fully aligned with Material accessibility/touch-target standards

## Migration principles

1. Prefer small, reversible steps.
2. Change theme/tokens before changing complex screens.
3. Preserve stable IDs where possible to reduce test churn.
4. Migrate low-risk screens first.
5. Do not touch the core quiz flow until shared styles and tokens are stable.
6. Prefer semantic design tokens over hardcoded visual values.
7. Keep UI tests running throughout the migration.

## Out of scope

- full rewrite to Jetpack Compose
- large navigation architecture rewrite
- feature redesign unrelated to Material alignment

---

## Phase 1 — Theme and design token foundation

### Goal

Create a reusable visual foundation before migrating screens.

### Files in scope

- `app/src/main/res/values/styles.xml`
- `app/src/main/res/values/colors.xml`
- `app/src/main/res/values/dimens.xml`
- `app/src/main/res/values-normal/dimens.xml`
- `app/src/main/res/values-small/dimens.xml`
- `app/src/main/res/values-v31/themes.xml`
- relevant drawables under `app/src/main/res/drawable/`

### Tasks

- define semantic color roles instead of raw names like `red`, `blue`, and `lightGray`
- introduce a spacing scale (`xs`, `sm`, `md`, `lg`, `xl`)
- introduce reusable text appearance styles for titles, section headers, labels, and body text
- review shape styling and rounded-corner consistency
- reduce direct use of hardcoded colors in drawables
- review DayNight compatibility for surfaces, outlines, and text contrast
- review status/navigation bar handling in `values-v31/themes.xml`

### Deliverables

- documented spacing/color/text style tokens
- updated theme foundation ready for screen migration
- fewer hardcoded values in shared resources

### Risks

- dark theme regressions if semantic replacements are incomplete
- visual mismatch if old drawables and new tokenized styles coexist too long

---

## Phase 2 — Structural cleanup across layouts

### Goal

Remove layout patterns that conflict with Material migration and accessibility.

### Files in scope

- `app/src/main/res/layout/main_activity.xml`
- `app/src/main/res/layout/about_fragment.xml`
- `app/src/main/res/layout/settings_fragment.xml`
- `app/src/main/res/layout/handbook_fragment.xml`
- `app/src/main/res/layout/declension_quiz_fragment.xml`
- `app/src/main/res/layout/row_case.xml`
- `app/src/main/res/layout/words_with_errors_fragment.xml`

### Tasks

- replace `Left` / `Right` constraints with `Start` / `End`
- replace `layout_marginLeft` / `layout_marginRight` with `layout_marginStart` / `layout_marginEnd`
- remove raw colors from layout files
- replace repeated inline spacing values with dimens tokens
- add or clarify accessibility metadata such as `contentDescription`
- review fixed sizes that may break on font scaling or small screens

### Deliverables

- RTL-safe layouts
- fewer inline constants
- better accessibility baseline

### Risks

- minor layout shifts on existing screens
- increased UI test brittleness if view positions change significantly

---

## Phase 3 — Migrate low-risk screens first

### Goal

Create reference implementations for the new Material direction.

### Files in scope

- `app/src/main/res/layout/about_fragment.xml`
- `app/src/main/res/layout/settings_fragment.xml`
- optionally `app/src/main/res/layout/main_activity.xml` if app shell updates are included

### Tasks for `about_fragment.xml`

- replace legacy `Button` widgets with `MaterialButton`
- remove custom button background usage where Material button styles are sufficient
- align spacing and typography with new tokens
- decide whether the app logo is decorative or informative and set `contentDescription` accordingly

### Tasks for `settings_fragment.xml`

- improve section hierarchy with text appearances
- review whether selection controls should remain radio/checkbox based or move toward Material-aligned controls
- improve spacing and touch-target consistency

### Deliverables

- one or two screens that demonstrate the target visual system
- reusable examples for future screen migration

### Risks

- low; these are good first migration targets

---

## Phase 4 — Shared component modernization

### Goal

Update reusable UI building blocks before touching the complex quiz screens.

### Files in scope

- `app/src/main/res/layout/row_case.xml`
- `app/src/main/res/values/styles.xml`
- drawables such as:
  - `button_background.xml`
  - `radio_button_background.xml`
  - `rounded_rect_blue.xml`
  - `rounded_rect_red.xml`
  - `rounded_rect_gray.xml`
  - `rect.xml`

### Tasks

- refactor `row_case.xml` to use more theme-aware styling
- review fixed widths and dense spacing in the case-row layout
- decide how to evolve `WordRadioButton` and related selection styling
- evaluate whether custom drawable states should be replaced by Material components or kept temporarily with better tokens

### Deliverables

- stable shared components compatible with the new theme/tokens
- reduced duplication of legacy styling

### Risks

- medium, because these components affect multiple screens at once
- possible Data Binding regressions if attributes or view structures are changed carelessly

---

## Phase 5 — Migrate complex content screens

### Goal

Bring the core learning flows in line with the new Material system without breaking behavior.

### Files in scope

- `app/src/main/res/layout/declension_quiz_fragment.xml`
- `app/src/main/res/layout/handbook_fragment.xml`
- `app/src/main/res/layout/words_with_errors_fragment.xml`
- related fragment/viewmodel code under `app/src/main/java/com/usharik/app/fragment/`
- custom layout code such as `app/src/main/java/com/usharik/app/widget/FlowLayout.java`

### Tasks

- apply new spacing, color, and typography tokens
- modernize selection controls where practical
- review `FlowLayout` sizing behavior and fixed heights
- make ad container surfaces consistent with the theme
- preserve IDs and binding assumptions used by app logic and UI tests

### Deliverables

- visually consistent quiz and handbook screens
- improved resilience for small screens, font scaling, and dark theme

### Risks

- high, because these are the most interactive screens
- drag/drop, animation, and view binding behavior may regress if migration is too aggressive

---

## Phase 6 — Navigation and app-shell polish

### Goal

Align app shell behavior and navigation visuals with the migrated screens.

### Files in scope

- `app/src/main/res/layout/main_activity.xml`
- `app/src/main/java/com/usharik/app/MainActivity.java`
- `app/src/main/res/layout/dialog_correct_answer.xml`

### Tasks

- decide whether to keep the current `DarkActionBar` setup temporarily or move earlier to a `MaterialToolbar`
- review drawer and navigation theming for consistency
- ensure edge-to-edge and window-inset handling matches the new surfaces and spacing
- use `dialog_correct_answer.xml` as the reference for button and dialog styling consistency

### Deliverables

- visually consistent app shell
- consistent toolbar, drawer, dialog, and inset behavior

### Risks

- medium; changes can affect all screens and navigation behavior

---

## UI test and QA strategy

### Requirements

- preserve important view IDs where possible
- run UI tests after each migration milestone
- update tests only when UI structure changes are intentional
- review screenshots and logs when layout changes occur

### Key references

- UI test workflow: `.github/workflows/ui-tests.yml`
- UI test log collection: `.github/scripts/run-ui-tests.sh`
- Copilot test report guidance: `.github/copilot-instructions.md`
- likely test entry point: `ui-tests/src/test/java/com/usharik/app/UiTests.java`

### Validation checklist per milestone

- app builds successfully
- main screens open without crashes
- light theme looks correct
- dark theme looks correct
- RTL layout remains valid
- UI tests pass or failures are explained by intentional UI changes

---

## Recommended first milestone

### Milestone 1 — Foundation + first visible Material win

#### Scope

- Phase 1 theme/token work
- RTL cleanup in the simplest screens
- migrate `about_fragment.xml`
- improve `settings_fragment.xml`
- keep quiz screens unchanged for now

#### Why this is the best starting point

- low risk
- visible improvement for users
- reusable styling patterns for later phases
- minimal impact on the quiz flow and UI tests

#### Success criteria

- theme tokens exist and are being used
- `about_fragment.xml` no longer relies on legacy `Button` styling
- `settings_fragment.xml` has clearer hierarchy and better Material consistency
- no regressions in navigation or core quiz behavior

---

## Suggested execution order

1. Theme, color, spacing, typography, and shape tokens
2. RTL and inline-value cleanup
3. `about_fragment.xml`
4. `settings_fragment.xml`
5. shared components such as `row_case.xml`
6. `handbook_fragment.xml`
7. `declension_quiz_fragment.xml`
8. `words_with_errors_fragment.xml`
9. navigation/app-shell polish

## Final note

Do not start the migration with `declension_quiz_fragment.xml` or `handbook_fragment.xml`.

The safest path is:

- establish tokens first
- migrate the simplest screens next
- modernize shared components
- only then update the main interactive learning flows

