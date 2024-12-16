# Threads Siege 🚀  

![Java](https://img.shields.io/badge/Java-8+-blue?logo=java&logoColor=white)
![Status](https://img.shields.io/badge/Status-Prototype-yellow)

An under 500 lines of code prototype **Threads Siege** made for a subject in uni, an arcade-style space defense game built with **Java Swing**. Players use their spaceship cannon to shoot down incoming alien enemies while avoiding letting too many enemies slip through.  

![Gameplay Screenshot](assets/gameplay_screenshot.png)
_**Screenshot 1.** Gameplay Screenshot_

⚠️ **Expect bugs and unpolished gameplay.** Contributions, feedback, and suggestions are highly welcome to help refine this game.  

---

## Features 🌟  

- **Multithreaded Mechanics**: Each enemy and bullet runs independently on its own thread, allowing real-time movement and interaction.  
- **Dynamic Difficulty Adjustment**: The number of active enemies and their spawn rate automatically increase as the game progresses, based on the player's score and elapsed time.  
- **Collision-Based Scoring**: Bullets destroy enemies on contact, incrementing the score while missed enemies contribute to the game-over counter.  
- **Limited Ammo**: Players can have only three active bullets at a time, managed using a semaphore for concurrency control.  
- **Game State Feedback**: The UI dynamically updates to reflect the player's score and missed enemies, with win/lose conditions displayed at game end.  

---

## Getting Started 🛠️  

### Prerequisites  

Ensure you have the following installed:  

- **Java 8** or higher  
- Any Java IDE or a command-line terminal  

### Installation  

1. **Clone the repository**:  
   ```bash  
   git clone https://github.com/raccoon-hero/threads-siege.git  
   cd threads-siege  
   ```  

2. **Compile the project**:  
   Using the terminal:  
   ```bash  
   javac ThreadsSiege.java  
   ```  

3. **Run the game**:  
   ```bash  
   java ThreadsSiege  
   ```  

---

## Controls 🎮  

- **Arrow Keys**: Move your cannon left and right.  
- **Spacebar**: Shoot bullets (limited to three active bullets at a time).  

---

## Project Structure 📂  

```plaintext  
threads-siege/  
├── assets/                     # Assets for showcasing gameplay (e.g., screenshots)  
├── ThreadsSiege.java           # Main game file  
└── README.md                   # Project Documentation (you’re here! :D)  
```  

---

## Known Issues 👾  

- **Thread Termination**: Active threads (e.g., enemies and bullets) are not guaranteed to terminate when the game exits, which may lead to memory leaks or lingering processes.  
- **Concurrency Management**: Shared resources like `enemies` and `bullets` are manually synchronized, which could cause subtle bugs under certain conditions.  
- **UI Scaling**: Game objects do not resize dynamically when the window size is changed, resulting in potential layout issues.  
- **Repaint Performance**: Under heavy load, the UI may experience performance drops due to frequent updates and rendering of multiple threads.  
- **Error Handling**: Exceptions such as `InterruptedException` are caught but not logged, which may hinder debugging.  

---

## Future Plans ⏩  

- **Thread Management**: Implement a proper cleanup mechanism for threads to ensure they terminate cleanly when the game ends.  
- **Improved Concurrency**: Replace manual synchronization with safer concurrent collections like `ConcurrentLinkedQueue` for managing enemies and bullets.  
- **Customizable Settings**: Introduce configuration options for adjusting game parameters such as enemy speed, bullet speed, and spawn rates.  
- **Graphics Scaling**: Add support for responsive scaling of game objects to adapt to window resizing.  
- **Performance Optimization**: Profile and optimize repaint intervals to reduce lag during heavy gameplay.  
- **Logging System**: Integrate basic logging to capture runtime exceptions and errors for debugging purposes.  

---

## Contributing 🤝  

1. Fork this repository.  
2. Create a feature branch (`git checkout -b feature/amazing-feature`).  
3. Commit your changes (`git commit -m 'Add some amazing feature'`).  
4. Push to the branch (`git push origin feature/amazing-feature`).  
5. Open a Pull Request. 