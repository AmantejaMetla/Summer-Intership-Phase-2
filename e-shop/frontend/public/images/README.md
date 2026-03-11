# Product images and logo

- **Logo:** Put your E-Shop logo here as **`logo.png`** (or `logo.svg`). It appears in the header beside "E-Shop". If you use a different name (e.g. `e-shop-logo.png`), update `frontend/src/components/Header.jsx` to use that path in the `<img src="..." />`.
- **Product images:** Use the **exact file names** listed in the project root file **IMAGES-FOR-FRONTEND.md** (e.g. `cappuccino-medium.jpg`, `espresso.jpg`, etc.). The app loads them at `/images/...` when the backend returns products.

## Hero showcase (home page only)

These are **separate from product images** and used only for the hero carousel:

| File name | Purpose |
|-----------|----------|
| **`homepage1-removebg-preview.png`** | First coffee showcase (rotates with 2 and 3) |
| **`homepage2-removebg-preview.png`** | Second coffee showcase |
| **`Homepage3-removebg-preview.png`** | Third coffee showcase (capital H must match filename) |

Put all three in **`frontend/public/images/`**.
