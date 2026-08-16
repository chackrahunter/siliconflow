# Workspace Rules: M3-Frametime Continuous Deployment & GitHub Releases

## Mandatory Workflow for Every Modification
Whenever any code, configuration, or documentation change is made to this codebase:

1. **Version Bump**:
   - Increment the mod version in `gradle.properties` (`mod_version=x.y.z`).
2. **Build & Auto-Deploy**:
   - Run `./gradlew build` to compile, test, and automatically sync the JAR into Prism Launcher.
3. **Git Commit & Push**:
   - Stage all modified files (`git add .`).
   - Create a clean semantic commit (`git commit -m "..."`).
   - Push immediately to GitHub (`git push origin main`).
4. **GitHub Release**:
   - Create or update the release on GitHub with `gh release create vX.Y.Z build/libs/m3-frametime-*.jar --clobber` or `gh release upload`.
