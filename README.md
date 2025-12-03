# SmartAir – Asthma Support App

SMART AIR is a kid-friendly Android app that helps children (ages 6–16) understand
asthma, practice good inhaler technique, log symptoms/medicine use, and share
parent-approved information with a healthcare provider via a concise, exportable report.

This project was developed as part of CSCB07 – Software Design (Fall 2025) at the University of Toronto Scarborough.

---

# 1. System Overview

SmartAir provides role-based access for Parents, Children, and Providers.  
The system integrates symptom tracking, medication logging, triage support, and selective data sharing to improve asthma care communication.

---

# 2. Architecture & Technical Design

SmartAir uses Model–View–Presenter (MVP) architecture to keep UI, business logic, and data layers clean and testable.

```
View (Activity/Fragment)
    → delegates logic to →
Presenter (Business logic)
    → requests data from →
Model (Firebase Auth/Firestore)
```

## Responsibilities

### View
- Displays UI
- Handles user input
- Delegates all logic to Presenter

### Presenter
- Input validation
- Login routing (parent/child/provider)
- Onboarding decisions
- Calls Model
- Unit-testable (no Android dependencies)

### Model
- Firebase Authentication
- Firestore read/write
- Invite code handling
- Returns results via callbacks

---

# 3. Key Modules

## 3.1 Authentication & Onboarding (R1)

Handles login, sign-up, password reset and first-time onboarding for all roles.

**Main responsibilities**
- Parent / Provider email-password login
- Child username-password login
- Role routing to Parent / Child / Provider homes
- Onboarding for first-time users

**Key components**
- `LoginActivity`
- `SignUpActivity`
- `ResetPasswordActivity`
- `OnboardingActivity`
- `LoginActivityPresenter`
- `LoginActivityModel`

---

## 3.2 Parent–Child Linking & Sharing (R2)

Supports managing children under a parent and controlling what is shared with providers.

**Main responsibilities**
- Add / manage child profiles under a parent
- Link children to provider accounts via invite
- Configure per-child sharing settings

**Key components**
- `AddChildActivity`
- `Child` (child data model)
- `ChildAdapter`
- `ChildDashboardHelper`
- `ChildHomeActivity`
- `ChildShareSettingsActivity`
- `ParentHomeActivity`
- `Parent_Setting_Activity`
- `ProviderInvite`
- `ProviderInviteAdapter`
- `ProviderListActivity`
- `ProviderInfo`
- `ProviderMainActivity`
- `PatientAdapter`

---

## 3.3 Medicines, Technique & Motivation (R3)

Manages medicine logging, inhaler technique guidance, and motivation features.

**Main responsibilities**
- Log rescue and controller medicines
- Show step-by-step inhaler technique helper
- Track inventory / expiry / low canister
- Calculate streaks and motivation badges

**Key components**
- `Medicine` (medicine data model)
- `MedicineLog`
- `MedicineLogActivity`
- `MedicineAdapter`
- `MedicineLogAdapter`
- `InventoryActivity`
- `TechniqueHelperActivity`
- `MotivationActivity`
- `MotivationCalculator`
- `ParentMotivationSettingsActivity`
- `ControllerSchedule`
- `ScheduleConfigurationActivity`

---

## 3.4 PEF, Zones & Triage (R4)

Implements peak-flow entry, zone computation, and the one-tap triage flow.

**Main responsibilities**
- Record PEF and personal best values
- Compute Green / Yellow / Red zones
- Guide users through triage steps with red-flag checks
- Escalate and notify parents when needed

**Key components**
- `PEFActivity`
- `TriageActivity`
- `ChildDashboardHelper`
- `ChartHelper`

---

## 3.5 Daily Check-In & History (R5)

Captures daily symptom control and allows browsing history with filters.

**Main responsibilities**
- Daily check-in for night waking, activity limits, cough/wheeze
- Trigger tagging (exercise, dust, cold air, etc.)
- History browsing with filters and export

**Key components**
- `DailyCheckInActivity`
- `HistoryActivity`
- `ChartHelper`

---

## 3.6 Parent Dashboard & Provider Report (R6)

Provides at-a-glance status for parents and a provider-ready summary.

**Main responsibilities**
- Show tiles for today’s zone, last rescue use, weekly rescue count
- Display short-term charts (7/30 days)
- Generate provider-ready report over a selected window

**Key components**
- `ParentHomeActivity`
- `ProviderHomeActivity`
- `ProviderReportActivity`
- `ChartHelper`
- `ChildDashboardHelper`

---

# 4. Design Choices

- **MVP** → high testability and separation of concerns
- **Firebase** → built-in Auth & real-time Firestore sync
- **Provider read-only** → safe access
- **Granular sharing** → parent privacy control
- **Child account without email** → easier for kids

---

# 5. Assumptions

- Parent may have multiple children, but each child can only have 1 parent

- Invite code cannot be revoked after provider accepts invitation

- PEF and PB are manually entered

- Motivation streaks: a streak day counts when the user takes exactly the planned amount of controller medication for that day.

- Each day can have only one Daily Check-in in total. Thus, the Daily Check-in data will never conflict, regardless of who submits it.

- A Planned Controller Day is considered completed only when the child takes exactly the number of controller doses specified in their parent’s controller schedule for that day.
  Taking fewer or more than the scheduled doses does not count as a completed day.

- A Technique Completed Day contributes to the technique streak only if the child is scheduled to take controller medication on that day.
  If the controller schedule for that day is 0, then: (1) The child is not required to complete a technique session, and (2) The technique streak remains unchanged.

- Both controller streaks and technique streaks are calculated using Schedule Days, not natural calendar days.
  Only days where the parent has scheduled a controller dose are considered when determining whether a streak should increase or break.

- A High Quality Technique Session is counted when the technique session is explicitly marked as "high-quality".
  The total number of such sessions is accumulated across all history.
  If the accumulated count meets or exceeds the parent-configured threshold, the child earns the High Quality Technique badge.

---

# 6. Sample Credentials (for TA grading)

### Parent
Email: `doraz000@163.com`  
Password: `sample`

### Provider
Email: `sample@pro.com `  
Password: `sample`

### Child
Username: `sample`  
Password: `sample`

---

# 7. Unit Testing – Login Module

The login module was refactored into MVP and the LoginActivityPresenter was unit-tested using JUnit4 + Mockito.

### Scope
Only Login Presenter was tested.  
No tests were written for other modules.

### Coverage includes:
- Missing credentials (adult/child)
- Mixed input (adult + child)
- Adult login → parent/provider/home/onboarding
- Child login success/failure
- Callback handling

---

# 8. Technology Stack

- Java
- Firebase Auth + Firestore + Cloud Messaging
- MPAndroidChart
- MVP Architecture
- JUnit4 + Mockito
- Android Studio

---

# 9. Scrum Process Summary

- 3 sprints
- sprint 1: R1, R2
- sprint 2: R3, R4, R5
- sprint 3: R6, final revision and testing
- Jira used for backlog & story points
- Standups documented: 3 each week
- Team agreement submitted

---


# 11. Team Members (Alphabetical by Last Name)

- Adam Belquas
- Lemeng Wang
- Buchao Yang
- Jiayi Zhang
- Zhuorong Zhao

---
