# College Quiz

A complete Java Spring Boot quiz web application featuring an intermediate college-level challenge, a live scoreboard, a dark premium theme, and an anti-repeat question engine.

## Features
- **10-Question Rounds**: Every attempt pulls 10 intermediate-level college questions (Math, Physics, CS, Economics, etc.).
- **Anti-Repeat Engine**: The backend keeps track of recent attempts to prevent repeating the same questions consecutively.
- **Real-time Score & Skip**: Header UI elements that allow users to skip questions and track their scores live.
- **Premium Jet-Black UI**: Custom-built HTML/CSS frontend with a modern glowing gradient background, silver-fade hover effects, and CSS animations.

## Tech Stack
- **Backend**: Java 17+, Spring Boot
- **Frontend**: Vanilla HTML5, CSS3, JavaScript (Fetch API)

## How to Run Locally

You must have **Java Development Kit (JDK) 17+** and **Maven** installed on your system.

1. **Clone the repository**:
   ```bash
   git clone https://github.com/souravsamaddar77-eng/project_2.git
   cd project_2
   ```

2. **Build the Application**:
   ```bash
   mvn clean package
   ```

3. **Run the Application**:
   ```bash
   java -jar target/college-quiz-0.0.1-SNAPSHOT.jar --server.port=8080
   ```

4. **Visit the Website**:
   Open a browser and go to:
   - [http://localhost:8080](http://localhost:8080)

## Important Note on Hosting
GitHub only hosts the **source code** for this project. Because this website requires a running Java Spring Boot backend to serve the API (`/api/quiz/start`), it **cannot** be played directly on GitHub Pages. To make the website accessible to the public, you must deploy this repository to a cloud hosting platform that supports Java applications, such as Render, Railway, AWS Elastic Beanstalk, or Azure App Service.
