# SQMan Implementation Progress

## Completed Features

### ✅ Download Command (Fully Implemented)

The download command has been fully implemented based on your old Python version, with improvements:

**Features:**
- Downloads SonarQube distributions from `binaries.sonarsource.com`
- Supports all editions: Community, Developer, Enterprise, DataCenter
- Resolves "latest" version automatically using GitHub API
- Progress bar with download percentage and MB indicators
- Automatic extraction and folder renaming
- Cleanup of ZIP files after extraction
- Smart version naming (handles versions < 4.0 with "sonar-" prefix)

**New Classes:**
- `DownloadService.java` - Handles download, extraction, and file management
- `InstanceService.java` - Manages installed instances

**Usage Examples:**
```bash
# Download latest version (any edition)
sqman download latest
sqman download latest -e developer
sqman download latest -e enterprise

# Download specific version
sqman download 10.3.0.82913
sqman download 9.9.0.65466 -e community

# Custom target directory
sqman download latest -d ~/my-sonarqube
```

**Default Installation Location:** `~/.sqman/`

---

### ✅ Versions Command (Already Working)

Lists available versions from GitHub's private sonar-enterprise repository.

**Usage:**
```bash
sqman versions                    # All editions
sqman versions -e community -l 5  # Top 5 community versions
sqman versions -e enterprise -l 0 # All enterprise versions
```

---

### ✅ List Command (Fully Implemented)

Lists all locally installed SonarQube instances.

**Features:**
- Scans `~/.sqman/` directory
- Shows installed versions
- Helpful message when no instances found

**Usage:**
```bash
sqman list
```

---

### ✅ Install Plugin Command (Fully Implemented)

Install custom plugin JARs into SonarQube instances with intelligent backup.

**Features:**
- Interactive instance selection from installed instances
- Validates JAR file existence and format
- **Smart plugin matching** - Recognizes different versions of same plugin (e.g., `sonar-java-plugin-8.22.0.jar` vs `sonar-java-plugin-SNAPSHOT.jar`)
- **Backup only once** - Backs up ORIGINAL plugin from SonarQube distribution, skips backup on subsequent installs
- Timestamped backups stored in `original-plugins/` directory
- Removes old plugin version before installing new one
- Warns if instance is running and needs restart

**New Classes:**
- `InstallPluginCommand.java` - Handles plugin installation with backup logic

**Usage Examples:**
```bash
# Install a locally built plugin (interactive mode)
sqman plugin ~/sonar-java-plugin/target/sonar-java-plugin-8.0.0.jar

# The command will:
# 1. Show list of installed instances
# 2. Prompt for selection
# 3. Backup existing plugin if present
# 4. Install new plugin
# 5. Remind to restart if instance is running
```

**Workflow:**
1. Build your plugin locally
2. Run `sqman plugin path/to/plugin.jar`
3. Select target instance from list
4. If first install: backs up original plugin → removes old → installs new
5. If subsequent install: skips backup → removes old → installs new
6. Restart instance if running

**Smart Behavior:**
- Extracts base plugin name to match across versions
- `sonar-java-plugin-8.22.0.41895.jar` → base: `sonar-java-plugin`
- `sonar-java-plugin-SNAPSHOT.jar` → base: `sonar-java-plugin`
- `my-custom-plugin-1.0.0.jar` → base: `my-custom-plugin`
- Checks if backup already exists for this base name
- Only backs up original once to preserve distribution state

---

### ✅ Restore Plugin Command (Fully Implemented)

Restore backed-up original plugins to SonarQube instances.

**Features:**
- Interactive instance selection
- Lists all backed-up plugins in `original-plugins/` directory
- Interactive backup selection
- Removes currently installed version before restoring
- Extracts original plugin name from backup filename
- Warns if instance is running and needs restart

**New Classes:**
- `RestorePluginCommand.java` - Handles plugin restoration from backups

**Usage:**
```bash
# Interactive mode (recommended)
sqman restore

# Flow:
# 1. Select instance
# 2. Select backed-up plugin to restore
# 3. Confirm and restore
```

**Workflow:**
1. Shows list of installed instances
2. Select target instance
3. Shows backed-up plugins in `original-plugins/` directory
4. Select plugin to restore
5. Removes any existing version of the plugin
6. Restores the backed-up plugin to `lib/extensions`
7. Reminds to restart if instance is running

**Smart Behavior:**
- Parses backup filename to extract original name
- `sonar-java-plugin-8.22.0.41895-backup-20260219-153045.jar` → restores as `sonar-java-plugin-8.22.0.41895.jar`
- Uses base name matching to find and remove current plugin version
- Backup file is DELETED after successful restoration
- Empty backup directory = no backups available (clear state)
- Next custom plugin install will create a fresh backup

---

### ✅ Run Command (Fully Implemented)

Start a SonarQube instance locally with **automatic first-time setup**.

**Features:**
- Platform detection (Windows, Linux, macOS)
- Background/detached mode
- Interactive instance selection
- Runs on default port 9000
- PID file management
- **Automatic first-time setup** (NEW!)
  - Detects if instance has been configured before (checks for `token` file)
  - Waits for SonarQube to be fully operational
  - Automatically changes admin password to `SqmanPsw123!`
  - Generates a global analysis token with no expiration
  - Saves token to `{instance-root}/token` file
  - Logs all credentials and token to console

**New Classes:**
- `SonarQubeSetupService.java` - Handles automatic SonarQube configuration via REST API

**Usage Examples:**
```bash
# Run instance (interactive selection)
sqman run

# Run specific version
sqman run 26.2.0.119303

# Run by index
sqman run 1

# First time running an instance - automatic setup happens:
# 1. Instance starts
# 2. Waits for SonarQube to be ready
# 3. Changes admin password
# 4. Generates global analysis token
# 5. Saves token to instance folder
# 6. Displays credentials and token
```

**Automatic Setup Flow:**
1. Instance starts in background
2. Checks if `token` file exists in instance directory
3. If not exists (first run):
   - Polls `/api/system/status` until SonarQube is UP
   - Calls `/api/users/change_password` to change admin password
   - Calls `/api/user_tokens/generate` to create global analysis token
   - Saves token to `{instance-root}/token`
   - Displays all credentials and token information
4. If exists (subsequent runs): skips setup

**Token Storage:**
- Location: `~/.sqman/sonarqube-{version}/token`
- Type: Global Analysis Token
- Expiration: Never expires
- Name: `sqman-global-analysis-token`

**Default Credentials After Setup:**
- Username: `admin`
- Password: `SqmanPsw123!`

---

## Pending Implementation

### ⚠️ Stop Command

Stop a running SonarQube instance.

**Inspiration from old Python version:**
- Read PID from `sq_pid.txt`
- Use process management to terminate
- Graceful shutdown with timeout
- Force kill if needed
- Cleanup PID file

**TODO:**
1. Read PID file
2. Find process by PID
3. Graceful termination
4. Force kill after timeout
5. Cleanup

---

### ⚠️ Config Command

Configure SonarQube instance settings.

**TODO:**
1. Read/write `sonar.properties` file
2. Support key-value pairs
3. Show all config when no args
4. Validate configuration keys

---

## Key Differences from Old Python Version

### Improvements ✨

1. **Type Safety**: Java provides compile-time type checking
2. **Better Structure**: Separated concerns (services vs commands)
3. **Edition Support**: First-class Edition enum with all 4 editions
4. **Modern HTTP**: Using OkHttp instead of Python requests
5. **Better Error Handling**: More specific exception types
6. **GitHub Integration**: Uses private enterprise repo for all editions

### Maintained Compatibility 🔄

1. **Same directory structure**: `~/.sqman/`
2. **Same file naming**: `sonarqube-{version}/`
3. **Same download sources**: `binaries.sonarsource.com`
4. **Same version format**: Major.Minor.Patch.Build

---

## Testing the Download Command

### Quick Test (Recommended)

```bash
# Build the project
mvn clean package -DskipTests

# Test with a specific older version (faster download)
java -jar target/sqman.jar download 9.9.0.65466 -e community

# Or download latest
java -jar target/sqman.jar download latest

# List installed instances
java -jar target/sqman.jar list
```

### Setting Up Alias

```bash
# Add to ~/.zshrc or ~/.bashrc
alias sqman='java -jar /Users/leonardo.pilastri/git/sqman/target/sqman.jar'

# Then use directly
sqman download latest
sqman list
sqman versions -e developer
```

---

## Next Steps

1. **Test Download**: Try downloading a SonarQube instance
2. **Implement Run**: Start instances with proper process management
3. **Implement Stop**: Clean shutdown of running instances
4. **Implement Config**: Manage sonar.properties
5. **Add Tests**: Unit tests for services and commands
6. **Documentation**: Update README with new features
7. **CI/CD**: Add GitHub Actions for automated builds

---

## Architecture

```
com.sqman/
├── SQManCLI.java                    # Main CLI entry point
├── model/
│   ├── Edition.java                 # Edition enum (4 editions)
│   └── SonarQubeVersion.java        # Version model with type (SQCB/SQS)
├── service/
│   ├── SonarQubeVersionService.java # Fetch versions from binaries website
│   ├── DownloadService.java         # ✅ Download & extract
│   ├── InstanceService.java         # ✅ Manage instances
│   ├── ProcessService.java          # ✅ Start/stop SonarQube processes
│   └── SonarQubeSetupService.java   # ✅ NEW: Automatic first-time setup
└── commands/
    ├── VersionsCommand.java         # ✅ List available versions
    ├── DownloadCommand.java         # ✅ Download distributions
    ├── ListCommand.java             # ✅ List installed instances
    ├── RunCommand.java              # ✅ UPDATED: Start instances with auto-setup
    ├── StopCommand.java             # ✅ Stop instances
    ├── DeleteCommand.java           # ✅ Delete instances
    ├── InstallPluginCommand.java    # ✅ Install custom plugins
    ├── RestorePluginCommand.java    # ✅ Restore original plugins
    └── ConfigCommand.java           # ⚠️  TODO: Configure instances
```

---

## Dependencies

- **Picocli 4.7.5**: CLI framework
- **OkHttp 4.12.0**: HTTP client
- **Jackson 2.16.1**: JSON processing
- **SLF4J 2.0.9**: Logging
- **Java 17+**: Required runtime

---

## Environment Requirements

- **Java 17+**: Required to run
- **Maven 3.6+**: Required to build
- **GITHUB_TOKEN**: Required for versions command (access to private sonar-enterprise repo)

---

## Build Info

```bash
# Clean build
mvn clean compile

# Package
mvn package -DskipTests

# Run tests
mvn test

# Create executable JAR
# Output: target/sqman.jar
```
