# SmartAir – Asthma Support App

SmartAir is an Android application designed to support children with asthma, their parents, and healthcare providers.  
The app enables symptom tracking, medication logging, triage assistance, and parent-controlled data sharing, following the official requirements from R1–R6 of the CSCB07 course project.

This project was developed as part of SCB07 – Software Design (Fall 2025) at the University of Toronto Scarborough.

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

# 5. Assumptions *to be added

- Parent may have multiple children, but each child can only have 1 parent
- Invite code cannot be revoked after provider accepts invitation
- PEF and PB are manually entered
- Internet required

---

# 6. Sample Credentials (for testing)

Replace with real test accounts if needed.

### Parent
Email: `parent@test.com`  
Password: `123456`

### Provider
Email: `provider@test.com`  
Password: `123456`

### Child
Username: `kidUser`  
Password: `kidPass`

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

- 3 sprints - 
- sprint 1: R1, R2
- sprint 2: R3, R4, R5, R6 
- sprint 3: final revision and testing
- Jira used for backlog & story points
-  Standups documented: 3 each week
- Team agreement submitted

---

# 10. Team Members (Alphabetical by Last Name)

- Adam Belquas
- Lemeng Wang
- Buchao Yang
- Jiayi Zhang
- Zhuorong Zhao

---
