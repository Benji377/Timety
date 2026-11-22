# Contributing to Timety

First off, thank you for considering contributing to Timety! It's people like you that make the open-source community such an incredible place to learn, inspire, and create. 

As a solo developer, I deeply appreciate any help, whether it's fixing a typo, squashing a bug, or proposing a massive new feature.

The following is a set of guidelines for contributing to Timety. These are mostly guidelines, not rules. Use your best judgment, and feel free to propose changes to this document in a pull request.

## Where do I go from here?

* **I have a question or an idea:** Please head over to our [GitHub Discussions](https://github.com/Benji377/Timety/discussions). It's the best place to ask how things work, pitch a new feature, or show off your setup.
* **I found a bug:** Check the existing [Issues](https://github.com/Benji377/Timety/issues) to see if it has already been reported. If not, open a new issue and include as much detail as possible (device, OS, steps to reproduce).
* **I want to write some code:** Look at the [Project Board](https://github.com/users/Benji377/projects/7) or Issues labeled `good first issue` or `help wanted`.

## The Golden Rule: Discuss before you build
If you want to add a large new feature or significantly refactor the codebase, **please open an Issue or a Discussion first**. 

Because Timety is carefully designed around an offline, privacy-first, Neo-brutalist philosophy, I want to make sure your hard work aligns with the project's roadmap before you spend hours coding it. 

## Local Development Setup

Technical Wiki indexed on: [DeepWiki](https://deepwiki.com/Benji377/Timety)

[![Architecture diagram of benji377/timety](https://gitdiagram.com/benji377/timety/diagram.png)](https://gitdiagram.com/benji377/timety?utm_source=readme&utm_medium=picture)

### Setting up the Environment

1. Ensure you have the **Android Studio** and **Java 17** installed.
2. Clone the repository.
3. Open the project in Android Studio to sync dependencies.
4. If modifying Room database entities, ensure schemas are updated correctly.
5. Run the app via Android Studio or `./gradlew installDebug`.

### Style Guide & Best Practices

* **Code Formatting**: We follow standard Kotlin style guidelines. 
  * Format your code using Android Studio's built-in formatter.
  * Check for lint errors: `./gradlew lintDebug`
  * Run the tests: `./gradlew testDebugUnitTest`

### User guides

The user guides live in [`docs/`](docs/). The [website](https://timety.benji377.dev/docs) renders them with [Starlight](https://starlight.astro.build/) and rebuilds whenever `docs/` changes on `main`. They're in English only; readers can use their browser's page translation.

* **Update the guide with the feature.** If a change alters how a feature works or looks, update its guide in the same pull request.
* **Structure:** folders are sidebar sections, files are pages, and an `index.md` is a section's landing page. `docs/README.md` is the landing page of the guides, and the order of its links is the order of the sidebar, so add a new page there too.
* **No front matter.** The `# Heading` is the page title, and the first paragraph is its description in search results and link previews. Keep that paragraph to one or two sentences about what the feature is for.
* **Write workflows,** not only screens and fields: "track chores you do a few times a week", then the steps.
* **Name things exactly as the app does,** in the same case, so readers can find them.
* **Markdown and HTML that GitHub keeps.** Pages must read well both on GitHub and on the website, so use only HTML that GitHub doesn't strip: for example `img` with `width`, `picture`, `table`, `details` and `kbd`, but no `style` or `script`. You may add `class` attributes: GitHub drops them, and the website uses them for styling. For notes, use GitHub's alerts (`> [!NOTE]`, `> [!TIP]`, `> [!WARNING]`). Link to other pages with relative `.md` paths.
* **Images** all go in `docs/images/`, named `<page>-<what>.png` after the page that uses them (for example `habit-stacks-overview.png`). A screenshot that several pages use is named after its screen instead, for example `settings-forms.png`. Crop screenshots to the part that matters and give them a `width` and an `alt` text.

## Pull Request Process

When you are ready to submit your code, please follow these steps:

1. **Create a branch** for your feature or bugfix (e.g., `feature/awesome-new-widget` or `bugfix/fix-calendar-crash`).
2. **Write clean, readable code.**
3. **Run the Linter & Tests:** Ensure your code passes our CI requirements locally before pushing.


3. **Update the UI (if applicable):** If you changed how the app looks, please include a screenshot or screen recording in your Pull Request description.
4. **Open a Pull Request** against the `main` branch.

I will review your PR as soon as I can. Please be patient, as I am managing this project in my free time!

## Code of Conduct

By participating in this project, you agree to abide by the Timety [Code of Conduct](CODE_OF_CONDUCT.md).
