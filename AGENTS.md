# Workspace Rules: SiliconFlow Development & Release Workflow

## Development & Testing Workflow
Whenever any code, configuration, or documentation change is made:

1. **Local Compile & Auto-Deploy**:
   - Run `./gradlew build` to compile and automatically sync the JAR into Prism Launcher for immediate in-game testing.
   - Allow the user to test and verify in-game.

## GitHub Release Workflow (Only on User Confirmation)
**Do NOT push to GitHub or create GitHub releases automatically on every small change.**
Only when the user explicitly confirms that everything works perfectly (e.g. *"alles funktioniert perfekt"* or requests a release):

1. **Version Bump**:
   - Increment the mod version in `gradle.properties` (`mod_version=x.y.z+<minecraft-version>`).
2. **Compile & Deploy**:
   - Run `./gradlew build`.
3. **Git Commit & Push**:
   - Stage all modified files (`git add .`).
   - Create a clean semantic commit (`git commit -m "..."`).
   - Push to GitHub (`git push origin main`).
4. **GitHub Release**:
   - Create the release on GitHub with `gh release create vX.Y.Z build/libs/siliconflow-*.jar`.
