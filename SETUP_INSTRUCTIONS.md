# Setup Instructions - Elevator System Simulation

## Quick Start (5 minutes)

### Prerequisites Installation

#### macOS
```bash
# Install Java 21
brew install openjdk@21
echo 'export PATH="/opt/homebrew/opt/openjdk@21/bin:$PATH"' >> ~/.zshrc
source ~/.zshrc

# Install Node.js
brew install node

# Verify installations
java -version
node -v
npm -v
```

#### Windows
```bash
# Install Java 21
# Download from: https://www.oracle.com/java/technologies/downloads/
# OR use Chocolatey:
choco install openjdk21

# Install Node.js
choco install nodejs

# Verify installations
java -version
node -v
npm -v
```

#### Linux (Ubuntu/Debian)
```bash
# Install Java 21
sudo apt-get update
sudo apt-get install openjdk-21-jdk

# Install Node.js
curl -fsSL https://deb.nodesource.com/setup_20.x | sudo -E bash -
sudo apt-get install -y nodejs

# Verify installations
java -version
node -v
npm -v
```

---

## Installation Steps

### 1. Clone/Download Project

```bash
cd /path/to/projects
# If from git:
git clone <repository-url>
cd elevator-system

# If downloading as ZIP:
unzip elevator-system.zip
cd elevator-system
```

### 2. Build Backend

```bash
# Option A: Using Maven wrapper (Recommended)
./mvnw clean package -DskipTests

# Option B: Using system Maven
mvn clean package -DskipTests

# Output: target/elevator-system-0.0.1-SNAPSHOT.jar ✅
```

**Expected Output:**
```
[INFO] Building jar: /path/to/elevator-system/target/elevator-system-0.0.1-SNAPSHOT.jar
[INFO] BUILD SUCCESS
```

### 3. Build Frontend

```bash
cd elevator-ui

# Install dependencies
npm install

# Build production bundle
npm run build

# Output: dist/elevator-ui ✅
```

**Expected Output:**
```
Application bundle generation complete. [X.XXX seconds]
✔ Build at: [timestamp]
```

---

## Running the Application

### Option 1: Development Mode (Best for Testing)

#### Terminal 1: Start Backend
```bash
cd /path/to/elevator-system

# Start Spring Boot backend
java -jar target/elevator-system-0.0.1-SNAPSHOT.jar

# Expected output:
# [main] c.e.e.ElevatorSystemApplication : Started ElevatorSystemApplication in 2.345 seconds
```

**Verify Backend Running:**
```bash
curl http://localhost:8080/api/elevator/health

# Expected response:
# {"status":"UP","service":"Elevator System"}
```

#### Terminal 2: Start Frontend
```bash
cd /path/to/elevator-system/elevator-ui

# Start Angular development server
npm start
# OR
ng serve --open

# Expected output:
# ✔ Application bundle generation complete
# ➜  Local:   http://localhost:4200/
```

**Open Browser:**
```
http://localhost:4200
```

---

### Option 2: Production Mode

#### Build Everything
```bash
# Backend already built in step 2

# Build frontend
cd elevator-ui
npm run build

# Output directory: dist/elevator-ui/
```

#### Serve Both from Single Port

Create a simple Node.js server to serve frontend + proxy backend:

```bash
# Create server.js
cat > server.js << 'EOF'
const express = require('express');
const proxy = require('express-http-proxy');
const path = require('path');

const app = express();

// Serve frontend static files
app.use(express.static(path.join(__dirname, 'elevator-ui/dist/elevator-ui')));

// Proxy API calls to backend
app.use('/api', proxy('http://localhost:8080'));
app.use('/ws', proxy('http://localhost:8080', {
  ws: true
}));

// Fallback to index.html for SPA routing
app.get('*', (req, res) => {
  res.sendFile(path.join(__dirname, 'elevator-ui/dist/elevator-ui/index.html'));
});

app.listen(8000, () => {
  console.log('Server running on http://localhost:8000');
});
EOF

# Install express
npm install express express-http-proxy

# Start combined server
node server.js
```

Then open: `http://localhost:8000`

---

## Configuration

### Backend Configuration

Edit `src/main/resources/application.properties`:

```properties
# Building configuration
elevator.system.numberOfElevators=3
elevator.system.numberOfFloors=10
elevator.system.elevatorCapacity=10

# Timing configuration (in milliseconds)
elevator.system.floorTransitionTimeMs=1000
elevator.system.doorOpenCloseTimeMs=2000
elevator.system.doorStayOpenTimeMs=3000
elevator.system.elevatorTickIntervalMs=100

# Logging
logging.level.com.example.elevator_system=INFO
logging.level.org.springframework.web=WARN
```

### Frontend Configuration

Edit `elevator-ui/src/app/services/elevator.service.ts`:

```typescript
private apiUrl = 'http://localhost:8080/api/elevator';

// Change for production:
private apiUrl = 'https://api.yourdomain.com/api/elevator';
```

---

## Troubleshooting

### Issue: Backend won't start - Port 8080 already in use

**Solution 1: Find and kill process**
```bash
# macOS/Linux
lsof -i :8080
kill -9 <PID>

# Windows
netstat -ano | findstr :8080
taskkill /PID <PID> /F
```

**Solution 2: Use different port**
```bash
# Add to application.properties
server.port=8081

# Then access: http://localhost:8080/api/elevator/health
```

### Issue: Frontend can't connect to backend

**Check:**
```bash
# Verify backend is running
curl http://localhost:8080/api/elevator/health

# Check CORS is enabled in application.properties
# (Already configured in GlobalExceptionHandler and controllers)

# Check browser console for errors
# Open DevTools → Console tab
```

**Solution: Enable CORS for development**
```java
// In application.properties or application.yml
# Add:
server.error.include-message=always
```

### Issue: npm packages failing to install

**Solution: Clear npm cache**
```bash
npm cache clean --force
rm -rf node_modules package-lock.json
npm install
```

### Issue: Angular build failing

**Solution: Check Node version**
```bash
node -v  # Should be 18+
npm -v   # Should be 9+

# Update if needed:
npm install -g npm@latest
```

### Issue: Maven build failing

**Solution: Check Java version**
```bash
java -version  # Should show Java 21

# Check Maven
./mvnw -v
```

---

## Testing the System

### Test 1: Call Elevator

```bash
# Call elevator to floor 5 going UP
curl -X POST http://localhost:8080/api/elevator/call \
  -H "Content-Type: application/json" \
  -d '{"floor": 5, "direction": "UP"}'

# Response:
# {"success":true,"message":"Elevator called successfully","floor":5,"direction":"UP"}
```

### Test 2: Get System Status

```bash
curl http://localhost:8080/api/elevator/status | python3 -m json.tool

# Should show all elevators and floors
```

### Test 3: Select Floor from Inside

```bash
curl -X POST http://localhost:8080/api/elevator/0/select \
  -H "Content-Type: application/json" \
  -d '{"elevatorId": 0, "floor": 8}'

# Response:
# {"success":true,"message":"Floor selected successfully"}
```

### Test 4: Multiple Concurrent Calls

```bash
#!/bin/bash
for i in {1..20}; do
  floor=$((RANDOM % 10))
  direction=$((RANDOM % 2))
  dir="UP"
  [ $direction -eq 1 ] && dir="DOWN"
  
  curl -X POST http://localhost:8080/api/elevator/call \
    -H "Content-Type: application/json" \
    -d "{\"floor\": $floor, \"direction\": \"$dir\"}" &
done
wait

echo "All requests sent!"
```

---

## Performance Verification

### Check Backend Performance

```bash
# Time a single request
time curl http://localhost:8080/api/elevator/status

# Expected: <100ms

# Load test (requires Apache Bench)
ab -n 1000 -c 10 http://localhost:8080/api/elevator/health

# Expected results:
# Requests per second: 100+
# Mean time per request: <10ms
```

### Monitor System Resources

```bash
# macOS
top -l 1 | grep "java\|node"

# Linux
ps aux | grep "java\|node"
top -p $(pgrep -f "java\|node")

# Windows
tasklist | findstr "java node"
```

---

## Development Workflow

### Making Backend Changes

1. Edit source files in `src/main/java/`
2. Stop backend (Ctrl+C)
3. Rebuild: `./mvnw clean package -DskipTests`
4. Restart: `java -jar target/elevator-system-0.0.1-SNAPSHOT.jar`

### Making Frontend Changes

1. Edit source files in `elevator-ui/src/app/`
2. Angular dev server auto-recompiles
3. Browser auto-refreshes (hot reload)
4. Check browser console for errors

### Best Practices

```
✅ Do
├─ Test changes in dev mode first
├─ Check browser console for errors
├─ Look at backend logs for issues
└─ Commit code frequently

❌ Don't
├─ Edit built artifacts
├─ Skip tests in production builds
├─ Ignore console warnings
└─ Leave debug logs in code
```

---

## Deployment Checklist

### Pre-Deployment

- [ ] Run `npm run build` for frontend
- [ ] Run `./mvnw clean package` for backend
- [ ] Test in production mode locally
- [ ] Review all configuration values
- [ ] Check environment variables
- [ ] Verify HTTPS certificates (if needed)
- [ ] Test with real data

### Deployment Commands

```bash
# Production backend
java -jar target/elevator-system-0.0.1-SNAPSHOT.jar \
  -Dserver.port=8080 \
  -Delevator.system.numberOfElevators=5 \
  -Delevator.system.numberOfFloors=20

# With Docker
docker build -t elevator-system:1.0 .
docker run -p 8080:8080 elevator-system:1.0

# With Docker Compose
docker-compose up -d
```

### Post-Deployment

- [ ] Verify API endpoints responding
- [ ] Check WebSocket connectivity
- [ ] Monitor server resources
- [ ] Test with production data
- [ ] Monitor error logs
- [ ] Set up alerts

---

## Getting Help

### Check Logs

**Backend Logs:**
```bash
# While running, logs output to console
# Or save to file:
java -jar target/elevator-system-0.0.1-SNAPSHOT.jar > logs/backend.log 2>&1 &

# View logs:
tail -f logs/backend.log
```

**Frontend Logs:**
```bash
# Open browser DevTools: F12 or Ctrl+Shift+I
# Check:
# - Console tab for errors
# - Network tab for API calls
# - Application tab for WebSocket connections
```

### Debug Mode

**Backend Debug:**
```bash
java -Xdebug -Xrunjdwp:server=y,transport=dt_socket,address=5005 \
  -jar target/elevator-system-0.0.1-SNAPSHOT.jar
```

**Frontend Debug:**
```bash
ng serve --inspect-brk
# Then open: chrome://inspect
```

### Common Issues & Solutions

See **Troubleshooting** section above.

---

## Next Steps

1. ✅ System is running
2. 📖 Read `README.md` for feature overview
3. 🏗️ Read `ARCHITECTURE.md` for technical details
4. 🧪 Run test scenarios from **Testing the System** section
5. 🔧 Modify configuration and observe behavior
6. 📝 Review source code
7. 🚀 Deploy to your environment

---

**Document Version**: 1.0  
**Last Updated**: March 28, 2026  
**Status**: Complete

