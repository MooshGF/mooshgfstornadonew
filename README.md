# MooshGF's Tornado Mod — Forge 1.20.1

A source project for Minecraft Java Edition 1.20.1 with Forge. The included GitHub Actions workflow builds the mod and uploads the resulting JAR as an artifact.

## Build on GitHub (no local Gradle required)

1. Create a GitHub repository.
2. Upload the **contents** of this project folder to the repository root (including the hidden `.github` folder). Do not upload only the ZIP file.
3. Open the repository's **Actions** tab.
4. Select **Build MooshGF's Tornado Mod**.
5. Click **Run workflow**. A push to `main` or `master` also starts a build.
6. Open the completed workflow run and download `MooshGFsTornado-Forge-1.20.1` under **Artifacts**. Extract the downloaded artifact ZIP to get the `.jar`.

## Commands

- `/mooshgfstornado help`
- `/mooshgfstornado start normal 1` through `4`
- `/mooshgfstornado start supercell 1` through `4`
- `/mooshgfstornado capacity 1` through `4`
- `/mooshgfstornado stop`
- `/mooshgfstornado dominator 1` through `4`

## Notes

- Target: Minecraft Java Edition 1.20.1, Forge 47.4.10, Java 17.
- Tornado visuals use vanilla `CLOUD` particles and `ELECTRIC_SPARK` for supercells; they do not use the `SMOKE` particle.
- Dominators are decorative block-built mockups, not drivable vehicles.
- This project has not been compiled in this environment. The GitHub Actions run is the test of whether the current source compiles; if it fails, open the failed log for the exact error.
