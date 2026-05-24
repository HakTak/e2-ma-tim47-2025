# RPG Habit & Goal Tracker

An innovative Android application that combines habit tracking and personal goal management with RPG game mechanics to increase user motivation and consistency.

## 📋 Project Overview

This mobile application gamifies the process of building habits and achieving goals by implementing RPG (Role-Playing Game) elements. Users create daily tasks and habits that reward them with experience points (XP), unlock badges, earn equipment, and progress through levels. The app combines progress tracking with social features to boost engagement and accountability.

## ✨ Key Features

### 1. Account Management
- **User Registration**: Create account with email verification (24-hour activation link)
- **User Profile**: Display avatar, username, level, title, power points (PP), XP, coins, badges, and equipment
- **Profile Customization**: Change password and view personal statistics
- **QR Code**: Unique QR code for adding friends
- **Statistics Dashboard**: Interactive charts showing:
  - Days of active use
  - Task completion rates
  - Success streaks
  - Tasks by category
  - Average difficulty
  - Weekly XP progress
  - Special missions completed

### 2. Task Management
- **Task Creation**: Create one-time or recurring tasks with:
  - Category assignment
  - Frequency settings (daily, weekly intervals)
  - Difficulty levels (Very Easy: 1 XP, Easy: 3 XP, Hard: 7 XP, Extremely Hard: 20 XP)
  - Importance levels (Normal: 1 XP, Important: 3 XP, Extremely Important: 10 XP, Special: 100 XP)
  - Custom name, description, and execution time
  
- **Task Management**: 
  - View in calendar with color-coded categories
  - List view with tabs for recurring vs one-time tasks
  - Task status: Active, Completed, Failed, Paused, Cancelled
  - Can mark tasks up to 3 days retrospectively
  
- **Daily Quota System**: Limits on XP rewards:
  - Very Easy & Normal: max 5 per day
  - Easy & Important: max 5 per day
  - Hard & Extremely Important: max 2 per day
  - Extremely Hard: max 1 per week
  - Special: max 1 per month

### 3. Category Management
- Create custom task categories (health, learning, entertainment, cleaning, etc.)
- Assign unique colors to categories
- Modify colors anytime (no duplicate colors allowed)
- Delete categories only if no active tasks exist

### 4. Level Progression System
- **Level Requirements**: 
  - Level 1: 200 XP
  - Each subsequent level: `(Previous XP × 2) + (Previous XP ÷ 2)` (rounded to next 100)
  
- **Rewards per Level**:
  - PP (Power Points): 40 for Level 1, then `Previous PP + (3/4 × Previous PP)`
  - New title for each level
  - Coins upon boss defeat
  
- **Scaling**: Task XP increases with levels to maintain difficulty

### 5. Boss Battle System
- **HP Calculation**: Each boss has health points (Level 1: 200 HP, subsequent: `Previous HP × 2 + Previous HP ÷ 2`)
- **5 Attack Attempts**: Per battle, each dealing player PP damage to boss HP
- **Success Rate**: Based on task completion rate in current stage
- **Rewards**:
  - Coins: Level 1 = 200, each level = 20% more than previous
  - 20% chance for equipment drop (95% clothing, 5% weapons)
  - Reduced rewards if boss defeats player (50% at half HP)

- **Attack Methods**:
  1. Button tap attack
  2. Shake sensor attack (motion-based)

- **Visual Elements**:
  - Animated boss sprite
  - Health/Power bars
  - Attack counter (5/5)
  - Success probability display
  - Animated treasure chest reward reveal

### 6. Equipment System

**Potions** (One-time use, consumed after 1st battle):
- +20% PP = 50% of previous level's boss reward coins
- +40% PP = 70% of previous level's boss reward coins
- +5% permanent PP increase = 200% coins
- +10% permanent PP increase = 1000% coins

**Clothing** (Lasts 2 battles, stackable):
- Gloves: +10% strength
- Shield: +10% hit chance
- Boots: +40% extra attack (1 per pair)

**Weapons** (Permanent, upgradeable):
- Sword: +5% strength
- Bow & Arrow: +5% coin gain

**Shop**: Purchase potions and clothing; obtain weapons from boss defeats

### 7. Special Missions (Multiplayer)

**Alliance System**:
- Create or join alliances with friends
- Search users by username
- Add friends via QR code scanning
- Real-time alliance chat
- Only one active alliance per user

**Special Missions**:
- 2-week duration, started by alliance leader
- Shared boss with HP = 100 × number of members
- HP reduction through special tasks:
  - Shopping (max 5): 2 HP
  - Successful boss attacks (max 10): 2 HP
  - Easy/Normal task completion (max 10): 1 HP
  - Other task completion (max 6): 4 HP
  - Zero failed tasks: 10 HP
  - Daily messages in chat (per day): 4 HP

**Rewards upon victory**:
- Potion, clothing, and 50% of next level's boss coins per member
- Unique badge for completed special missions

## 🛠️ Technical Requirements

### Architecture
- **Three-layer architecture**: Presentation layer, Business logic, Data management layer
- **Language**: Java
- **Database**: SQLite + Firebase (Firestore/Realtime Database)
- **Preferences**: SharedPreferences for app settings
- **Notifications**: Android built-in notification system

### Required Libraries
- **ZXing QR Code Scanner**: QR code generation and scanning
- **Lottie**: Animations (chest opening, boss hit, confetti)
- **MPAndroidChart**: Statistics graphs and charts

### Validation
- Form field validation required
- Time-based requirements converted to seconds/minutes for demo purposes

## 📱 Installation & Setup

### Prerequisites
- Android Studio (latest version)
- Java JDK 11 or higher
- Android SDK 28 or higher
- Firebase project configuration

### Build & Run
1. Clone the repository
2. Open project in Android Studio
3. Configure `google-services.json` in `app/` folder with your Firebase credentials
4. Build: `./gradlew build`
5. Run on emulator or device: `./gradlew installDebug`

## 📁 Project Structure

```
e2-ma-tim47-2025/
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── AndroidManifest.xml
│   │   │   ├── java/          # Main application code
│   │   │   └── res/           # Resources (layouts, strings, drawables)
│   │   ├── test/              # Unit tests
│   │   └── androidTest/       # Instrumentation tests
│   ├── build.gradle
│   └── google-services.json
├── build.gradle
├── settings.gradle
├── gradle.properties
└── README.md
```

## 👥 Team Structure

**Student 1** - Responsible for:
- Account Management (1)
- Level Progression (4)
- Equipment System (6)
- Friends & Alliance Management (7.1)
- Alliance Chat (7.2)

**Student 2** - Responsible for:
- Task Management (2)
- Category Management (3)
- Boss Battle System (5)
- Special Alliance Missions (7.3)

## 🎮 Gameplay Flow

1. **Register & Setup**: Create account, select avatar, verify email
2. **Create Tasks**: Build habit list with difficulty and importance
3. **Complete Tasks**: Mark tasks done to gain XP
4. **Level Up**: Accumulate XP to reach new levels
5. **Boss Battles**: Defeat boss at each level to earn coins and equipment
6. **Equipment**: Use potions and equip clothing/weapons for advantage
7. **Form Alliances**: Team up with friends for special 2-week missions
8. **Track Progress**: Monitor statistics and achievement badges

## 📊 Progression Formulas

### XP Requirements
```
Level 1: 200 XP
Level N: (Previous × 2) + (Previous ÷ 2) [rounded up to 100]
```

### Power Points (PP)
```
Initial (Level 1): 40 PP
Level N: Previous + (¾ × Previous) [rounded]
```

### Boss Health (HP)
```
Level 1: 200 HP
Level N: (Previous × 2) + (Previous ÷ 2) [rounded up to 100]
```

### Task XP Scaling
Task difficulty and importance XP values increase similarly to PP formula with each level.

## 🔐 Security & Privacy

- Password change requires old password verification
- User data visible to others: avatar, username, level, title, QR code, XP, badges, current equipment
- Private data: password, email, coins, other stats
- 24-hour email verification window for account activation
- Alliance data only visible to members

## 🎨 UI/UX Features

- Color-coded task categories
- Interactive calendar view
- Real-time progress bars (HP, PP, XP)
- Animated reward system (chest opening, confetti)
- Visual boss battle interface
- Chart-based statistics visualization
- Night mode ready (recommended)

## 📝 Notes

- Time-dependent features are scaled for demonstration purposes
- Application follows Material Design principles
- Supports Android 9.0+ (API level 28+)
- Responsive design for various screen sizes

## 📄 License

This project is an academic assignment for the Mobile Applications course (2024/2025) at the Faculty of Organizational Sciences, University of Belgrade.

## 🤝 Contributing

This is a university project with defined team structure. Contributions should follow the task distribution outlined above.

## 📞 Support

For issues or questions related to this project, please contact the development team through the university course platform.

---

**Project Status**: In Development  
**Last Updated**: May 2026  
**Version**: 1.0.0
