# RepUp — demo notes

Short, plain-language explanations of each major part, for answering questions at the demo.

## Page layout and dark mode
Every page is drawn on the server by Thymeleaf from one shared template, `src/main/resources/templates/layout.html`. A page such as `index.html` only writes its own `<main>` content and passes in its title and description; the layout adds the `<head>` (SEO tags, canonical link, JSON-LD, fonts, CSS), a "Skip to content" link for keyboard users, the header and the footer. Dark mode follows the operating system on the first visit. A tiny script in `<head>` runs before anything is drawn and adds the `dark` class to `<html>` when needed, so the page never flashes white first. The moon/sun button in the header (`static/js/theme.js`) switches the theme and remembers the choice in the browser's `localStorage`. All colours are CSS variables in `src/main/frontend/app.css`, so switching the class changes every colour at once.
