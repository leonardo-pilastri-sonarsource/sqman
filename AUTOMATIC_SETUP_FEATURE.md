# Automatic Setup Feature - Implementation Summary

## Overview

The `sqman run` command now includes **automatic first-time setup** that eliminates the manual steps of changing the default admin password and generating an analysis token.

## What Was Implemented

### New Service: `SonarQubeSetupService`

**Location:** `src/main/java/com/sqman/service/SonarQubeSetupService.java`

**Responsibilities:**
1. Wait for SonarQube to be fully operational (polls `/api/system/status`)
2. Change the default admin password from `admin` to `SqmanPsw123!`
3. Generate a global analysis token with no expiration
4. Save the token to the instance directory
5. Display all credentials and token information to the user

### Modified: `RunCommand`

**Location:** `src/main/java/com/sqman/commands/RunCommand.java`

**Changes:**
- After starting an instance, checks if automatic setup is needed
- Setup is needed if the `token` file doesn't exist in the instance directory
- If needed, waits for SonarQube to be ready and performs automatic setup
- Handles errors gracefully (setup failure doesn't fail the run command)

## How It Works

### First Run of an Instance

```bash
sqman run 26.2.0.119303
```

**Output:**
```
Running SonarQube 26.2.0.119303

Starting SonarQube 26.2.0.119303...
Platform: MACOS
Script: /Users/you/.sqman/sonarqube-26.2.0.119303/bin/macosx-universal-64/sonar.sh

Started in background mode.
Waiting for PID file...
✓ SonarQube started with PID: 12345
✓ Check logs: /Users/you/.sqman/sonarqube-26.2.0.119303/sqman-startup.log

SonarQube is starting up...
Web UI will be available at: http://localhost:9000

Waiting for SonarQube to be fully operational...
  Still waiting... (10s elapsed)
  Still waiting... (20s elapsed)
✓ SonarQube is ready!

═══════════════════════════════════════════════════════════
  AUTOMATIC SETUP
═══════════════════════════════════════════════════════════

SQMan will now perform automatic configuration:
  • Change admin password to: SqmanPsw123!
  • Generate a global analysis token

Please wait...

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
  squ_a1b2c3d4e5f6g7h8i9j0k1l2m3n4o5p6q7r8s9t0

Token saved to: /Users/you/.sqman/sonarqube-26.2.0.119303/token

Use this token for analysis with:
  mvn sonar:sonar -Dsonar.token=squ_a1b2c3d4e5f6g7h8i9j0k1l2m3n4o5p6q7r8s9t0
```

### Subsequent Runs

```bash
sqman run 26.2.0.119303
```

Setup is **skipped** because the `token` file already exists. Instance starts normally without any additional configuration.

## Technical Details

### Detection Logic

Setup is needed if: `~/.sqman/sonarqube-{version}/token` file does **not** exist

This file acts as a marker that the instance has been configured.

### API Calls Made

1. **Wait for Ready State**
   - Endpoint: `GET /api/system/status`
   - Polls every 2 seconds (max 5 minutes)
   - Waits for `"status":"UP"` in response

2. **Change Admin Password**
   - Endpoint: `POST /api/users/change_password`
   - Authentication: Basic auth with `admin:admin`
   - Body: `login=admin&password=SqmanPsw123!&previousPassword=admin`

3. **Generate Token**
   - Endpoint: `POST /api/user_tokens/generate`
   - Authentication: Basic auth with `admin:SqmanPsw123!` (new password)
   - Body: `name=sqman-global-analysis-token&type=GLOBAL_ANALYSIS_TOKEN`
   - No `expirationDate` = token never expires

### Token Storage

- **Location:** `~/.sqman/sonarqube-{version}/token`
- **Format:** Plain text file containing just the token string
- **Type:** Global Analysis Token
- **Name:** `sqman-global-analysis-token`
- **Expiration:** Never expires

### Error Handling

If automatic setup fails:
- The instance **continues running** (setup failure doesn't stop the instance)
- A warning message is displayed
- User is instructed to configure manually at http://localhost:9000
- Default credentials reminder is shown

## Configuration

### Hardcoded Values

All configuration values are hardcoded in `SonarQubeSetupService.java`:

```java
private static final String DEFAULT_ADMIN_USERNAME = "admin";
private static final String DEFAULT_ADMIN_PASSWORD = "admin";
private static final String NEW_ADMIN_PASSWORD = "SqmanPsw123!";
private static final String TOKEN_NAME = "sqman-global-analysis-token";
private static final int MAX_WAIT_SECONDS = 300; // 5 minutes
private static final int POLL_INTERVAL_SECONDS = 2;
```

**To customize:**
- Edit `SonarQubeSetupService.java`
- Recompile: `mvn clean package -DskipTests`

## Testing

### Build and Test

```bash
# Build the project
mvn clean package -DskipTests

# Run a fresh instance (will trigger automatic setup)
java -jar target/sqman.jar run 26.2.0.119303

# Verify token file was created
cat ~/.sqman/sonarqube-26.2.0.119303/token

# Stop the instance
java -jar target/sqman.jar stop

# Run again (setup should be skipped)
java -jar target/sqman.jar run 26.2.0.119303
```

### Manual Testing Checklist

- [ ] First run triggers automatic setup
- [ ] Admin password is changed
- [ ] Token is generated
- [ ] Token is saved to file
- [ ] Credentials and token are displayed
- [ ] Subsequent runs skip setup
- [ ] Login works with new credentials (`admin / SqmanPsw123!`)
- [ ] Token works for analysis
- [ ] Error handling works if SonarQube takes too long to start

## Future Enhancements

Possible improvements:

1. **Configurable Password**
   - Add flag: `sqman run --password myCustomPassword`
   - Or read from config file

2. **Skip Setup Option**
   - Add flag: `sqman run --no-setup`
   - For users who want manual configuration

3. **Multiple Tokens**
   - Generate different token types (user token, project token)
   - Store in separate files

4. **Token Management Command**
   - `sqman token list` - Show all tokens for an instance
   - `sqman token regenerate` - Create new token
   - `sqman token revoke` - Revoke a token

5. **Additional Setup Steps**
   - Configure JDBC connection
   - Install default quality profiles
   - Create default projects

## Files Modified

1. **New:** `src/main/java/com/sqman/service/SonarQubeSetupService.java`
2. **Modified:** `src/main/java/com/sqman/commands/RunCommand.java`
3. **Updated:** `README.md` (documentation)
4. **Updated:** `IMPLEMENTATION.md` (implementation notes)

## Summary

The automatic setup feature provides a **zero-configuration experience** for first-time SonarQube instance usage:

✅ No manual password change needed
✅ No manual token generation needed
✅ Token saved automatically for easy access
✅ Ready to run analyses immediately
✅ Subsequent runs are instant (no setup delay)

This significantly improves the developer experience and reduces the time from download to first analysis.
