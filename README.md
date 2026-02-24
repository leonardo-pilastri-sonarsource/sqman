# SQMan - SonarQube Manager CLI

A CLI utility tool for managing SonarQube instances locally.

## Features

- **Download** SonarQube distributions (SQCB and SQS editions)
- **Run** SonarQube instances locally with **automatic first-time setup**
  - Automatically changes default admin password
  - Generates and saves analysis token
  - Ready to use immediately!
- **Stop** running instances
- **List** available and installed instances
- **Delete** local SonarQube installations
- **Install Plugin** Install custom plugin JARs with automatic backup
- **Restore Plugin** Restore backed-up original plugins
- **Configure** SonarQube settings easily

## Quick Start

```bash
# 1. Download a version
sqman download latest

# 2. Run it (interactive - just press Enter and select from list)
sqman run
# First run automatically configures SonarQube and generates a token!

# 3. Stop it when done
sqman stop

# Access SonarQube at http://localhost:9000
# Credentials: admin / SqmanPsw123!
# Token saved in: ~/.sqman/sonarqube-{version}/token
```

## Understanding SonarQube Editions

SonarQube has two main distribution types:
- **SQCB** (SonarQube Community Build) - Open source edition
- **SQS** (SonarQube Server) - Commercial editions (Developer, Enterprise, DataCenter)

All editions use the **same version numbers**. The edition (Community vs Server features) is determined by your **license file**, not the version you download.

## Requirements

- Java 17 or higher
- Maven 3.6+ (for building)
- **GITHUB_TOKEN** environment variable (**REQUIRED**)
  - Needed to access the private `sonar-enterprise` repository for version listings
  - Your GitHub account must have access to `SonarSource/sonar-enterprise`

## Setup

### 1. Set GitHub Token (Required)

```bash
export GITHUB_TOKEN=your_github_personal_access_token
```

To create a GitHub token:
1. Go to GitHub Settings → Developer settings → Personal access tokens → Tokens (classic)
2. Generate new token with `repo` scope (for private repository access)
3. Ensure your GitHub account has access to the `SonarSource/sonar-enterprise` repository
4. Copy the token and export it as shown above

### 2. Build the Project

```bash
mvn clean package
```

This will create an executable JAR: `target/sqman.jar`

## Usage

### Run the CLI

```bash
java -jar target/sqman.jar [command] [options]
```

Or create an alias for convenience:

```bash
alias sqman='java -jar /path/to/sqman.jar'
```

### Available Commands

#### List Available Versions

```bash
sqman versions [-l limit] [-t type]

# Examples:
sqman versions              # Show 10 latest versions (both SQCB and SQS)
sqman versions -l 20        # Show 20 latest versions
sqman versions -l 0         # Show all versions
sqman versions -t sqcb      # Show only SQCB (Community Build) versions
sqman versions -t sqs       # Show only SQS (Server) versions
```

**Note:** SQCB and SQS versions have different build numbers. Use SQCB versions for community edition downloads, and SQS versions for commercial edition downloads.

#### Download SonarQube

```bash
sqman download <version> [-e edition] [-d directory]

# Examples:
# Download latest SQCB (Community Build)
sqman download latest

# Download specific version for SQCB
sqman download 26.2.0.119303 -e community

# Download SQS (Server) editions
sqman download 26.2.0.119303 -e developer
sqman download 26.2.0.119303 -e enterprise
sqman download 26.2.0.119303 -e datacenter

# Download to custom directory
sqman download 10.7.0.96327 -d ~/sonarqube
```

#### Run SonarQube

```bash
sqman run [version|index]

# Interactive selection (no arguments)
sqman run                            # Shows list, prompts for selection

# Direct selection
sqman run 1                          # Run first instance from list
sqman run 26.2.0.119303              # Run specific version
sqman run 10.7                       # Partial version match
```

**Notes:**
- Runs on default port **9000** (http://localhost:9000)
- Always runs in **background mode**
- Only **one instance** can run at a time
- If an instance is already running, you must stop it first
- **First-time automatic setup** - On first run, automatically configures the instance

**Automatic First-Time Setup:**
When you run an instance for the first time, sqman will automatically:
1. Wait for SonarQube to be fully operational
2. Change the default admin password to `SqmanPsw123!`
3. Generate a global analysis token (no expiration)
4. Save the token to `~/.sqman/sonarqube-{version}/token`
5. Display all credentials and the token

Example first-run output:
```
✓ SonarQube started with PID: 12345
Waiting for SonarQube to be fully operational...
✓ SonarQube is ready!

═══════════════════════════════════════════════════════════
  AUTOMATIC SETUP
═══════════════════════════════════════════════════════════

SQMan will now perform automatic configuration:
  • Change admin password to: SqmanPsw123!
  • Generate a global analysis token

[1/2] Changing admin password...
      ✓ Admin password changed successfully

[2/2] Generating global analysis token...
      ✓ Token generated successfully

═══════════════════════════════════════════════════════════
  SETUP COMPLETE!
═══════════════════════════════════════════════════════════

Credentials:
  Username: admin
  Password: SqmanPsw123!

Global Analysis Token:
  squ_a1b2c3d4e5f6g7h8i9j0...

Token saved to: /Users/you/.sqman/sonarqube-26.2.0.119303/token

Use this token for analysis with:
  mvn sonar:sonar -Dsonar.token=squ_a1b2c3d4e5f6g7h8i9j0...
```

**Interactive Mode:**
When you run `sqman run` without specifying a version, it will show you all installed instances and prompt you to select one:
```
Installed SonarQube instances:

  1. 10.7.0.96327
  2. 2026.1.0.119033
  3. 26.1.0.118079

Select instance to run (1-3, or 0 to cancel):
```

#### Stop SonarQube

```bash
sqman stop [version|index] [-f]

# Stop the running instance (automatic detection)
sqman stop                    # Finds and stops the running instance

# Stop specific version
sqman stop 26.2.0.119303      # Stop specific version (if running)
sqman stop 10.7               # Partial version match
sqman stop --force            # Force kill the running instance
```

**Behavior:**
- **No argument**: Automatically finds and stops the running instance
- **With version**: Stops the specified version (if it's running)
- **--force**: Force kill if graceful shutdown fails

#### List Installed Instances

```bash
sqman list

# Example:
sqman list
```

**Tip:** The numbers shown in the list can be used directly with `run`, `stop`, and `delete` commands:
```bash
sqman list         # Shows: 1. 10.7.0.96327  2. 26.2.0.119303
sqman run 1        # Runs 10.7.0.96327
sqman stop 2       # Stops 26.2.0.119303
sqman delete 1     # Deletes 10.7.0.96327
```

#### Delete SonarQube Instance

```bash
sqman delete [version|index] [-f] [-y]

# Interactive selection (no arguments)
sqman delete                          # Shows list, prompts for selection

# Direct selection
sqman delete 1                        # Delete first instance from list
sqman delete 26.2.0.119303            # Delete specific version
sqman delete 10.7                     # Partial version match
sqman delete 26.2 --yes               # Skip confirmation prompt
sqman delete 26.2.0.119303 --force    # Force delete even if running

# Examples:
sqman delete                    # Interactive mode
sqman delete 26.2.0.119303      # Delete specific version (with confirmation)
sqman delete 1 -y               # Delete first instance without confirmation
sqman delete 10.7 -f            # Force delete (stops if running)
```

**Safety Features:**
- Prevents deletion of running instances (unless `--force` is used)
- Prompts for confirmation before deletion (unless `--yes` is used)
- Shows instance location and status before deletion

**Example with running instance:**
```
$ sqman delete 26.2.0.119303
Error: Instance 26.2.0.119303 is currently running.

Stop it first with:
  sqman stop 26.2.0.119303

Or use --force to stop and delete:
  sqman delete 26.2.0.119303 --force
```

#### Install Plugin

```bash
sqman install-plugin <path/to/plugin.jar>

# Examples:
sqman install-plugin ~/sonar-java-plugin/target/sonar-java-plugin-8.0.0.jar
sqman install-plugin /path/to/custom-plugin.jar
```

**How it works:**
1. You specify the path to your locally built plugin JAR
2. sqman shows a list of installed instances and prompts you to select one
3. sqman matches plugins by base name (e.g., `sonar-java-plugin` matches `sonar-java-plugin-8.22.0.jar`, `sonar-java-plugin-SNAPSHOT.jar`, etc.)
4. If an existing plugin is found AND no backup exists yet, it backs up the ORIGINAL plugin only (not subsequent user-installed versions)
5. Backups are stored in `<instance-root>/original-plugins/` with a timestamp
6. The old plugin is removed and the new plugin is installed to `lib/extensions`

**Example workflow:**
```bash
# Build your plugin locally
cd ~/sonar-java-plugin
mvn clean package

# Install it into a SonarQube instance
sqman install-plugin target/sonar-java-plugin-8.0.0.jar

# You'll see:
# Select target SonarQube instance:
#   1. 10.7.0.96327
#   2. 26.2.0.119303
# Select instance (1-2, or 0 to cancel): 2

# If plugin exists, it backs up the ORIGINAL (first time only):
# Found existing plugin: sonar-java-plugin-8.22.0.41895.jar
# Backing up ORIGINAL plugin to: sonar-java-plugin-8.22.0.41895-backup-20260219-153045.jar
# ✓ Original plugin backed up
# Removing existing plugin...
# Installing plugin to: /Users/you/.sqman/sonarqube-26.2.0.119303/lib/extensions/sonar-java-plugin-8.23.0.41992.jar
#
# ✓ Plugin installed successfully!

# On subsequent installs (backup already exists):
# Found existing plugin: sonar-java-plugin-8.23.0.41992.jar
# Backup already exists for this plugin (original already saved)
# Skipping backup...
# Removing existing plugin...
# Installing plugin...

# Restart instance if running
sqman stop
sqman run
```

**Notes:**
- Plugin matching is smart: `sonar-java-plugin-8.22.0.jar` and `sonar-java-plugin-SNAPSHOT.jar` are recognized as the same plugin
- Backups only happen ONCE per plugin - the original from the SonarQube distribution is preserved
- Subsequent installs skip backup to avoid cluttering the backup directory
- Backups include timestamps (format: `plugin-name-version-backup-YYYYMMDD-HHMMSS.jar`)
- If the instance is running, sqman reminds you to restart it
- Only `.jar` files are accepted

#### Restore Plugin

```bash
sqman restore-plugin

# Interactive flow:
# 1. Select instance
# 2. Select backed-up plugin to restore
```

**How it works:**
1. Shows list of installed instances - select target instance
2. Shows list of backed-up plugins in `original-plugins/` directory
3. Select which plugin to restore
4. Removes any currently installed version of that plugin
5. Restores the backed-up plugin to `lib/extensions`
6. Deletes the backup file (no longer needed since original is restored)

**Example workflow:**
```bash
sqman restore-plugin

# Step 1: Select instance
# Select SonarQube instance:
#   1. 10.7.0.96327
#   2. 26.2.0.119303
# Select instance (1-2, or 0 to cancel): 2

# Step 2: Select plugin to restore
# Available backed-up plugins:
#   1. sonar-java-plugin-8.22.0.41895-backup-20260219-153045.jar
#   2. sonar-python-plugin-4.5.0.12333-backup-20260219-160120.jar
# Select plugin to restore (1-2, or 0 to cancel): 1

# Restoration process:
# Found existing plugin: sonar-java-plugin-SNAPSHOT.jar
# Removing current plugin...
#
# Restoring plugin to: /Users/you/.sqman/sonarqube-26.2.0.119303/lib/extensions/sonar-java-plugin-8.22.0.41895.jar
# Deleting backup file...
#
# ✓ Plugin restored successfully!
#
# Instance: 26.2.0.119303
# Plugin: sonar-java-plugin-8.22.0.41895.jar
# Location: /Users/you/.sqman/sonarqube-26.2.0.119303/lib/extensions/sonar-java-plugin-8.22.0.41895.jar
#
# Note: Backup file has been deleted.
# If you install a custom plugin again, a new backup will be created.
#
# ⚠ Note: Instance is currently running.
# Restart the instance for changes to take effect:
#   sqman stop
#   sqman run 26.2.0.119303
```

**Use Cases:**
- Revert to the original plugin version that came with SonarQube
- Undo custom plugin installations
- Test with the original plugin after trying custom builds

**Notes:**
- Only backed-up plugins (in `original-plugins/` directory) can be restored
- The backup file is DELETED after successful restoration
- This prevents confusion - empty backup folder = no backups available
- If you install a custom plugin again, a fresh backup will be created
- If instance is running, you'll be reminded to restart it

#### Configure Instance (Not Yet Implemented)

```bash
sqman config <instance> [key] [value]

# Examples:
sqman config 26.2.0.119303
sqman config 26.2.0.119303 sonar.jdbc.url jdbc:postgresql://localhost/sonar
```

## Project Structure

```
sqman/
├── pom.xml                          # Maven configuration
├── README.md                        # This file
└── src/
    └── main/
        └── java/
            └── com/
                └── sqman/
                    ├── SQManCLI.java           # Main entry point
                    └── commands/                # Command implementations
                        ├── DownloadCommand.java
                        ├── RunCommand.java
                        ├── StopCommand.java
                        ├── ListCommand.java
                        └── ConfigCommand.java
```

## Development

### Adding New Commands

1. Create a new class in `src/main/java/com/sqman/commands/`
2. Annotate with `@Command` from Picocli
3. Implement `Callable<Integer>`
4. Add the command to `SQManCLI` subcommands list

### Running During Development

```bash
mvn clean compile exec:java -Dexec.mainClass="com.sqman.SQManCLI" -Dexec.args="[your args]"
```

## License

MIT License - feel free to use and modify as needed.
