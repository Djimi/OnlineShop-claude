# 🔧 Fixing Blank Page Issue

If you're seeing a blank page, follow these steps:

## Step 1: Stop the Dev Server

Press `Ctrl+C` in the terminal running `npm run dev`

## Step 2: Clear Cache and Reinstall

```bash
# From frontend directory
rm -rf node_modules package-lock.json
npm install
```

## Step 3: Clear Browser Cache

1. Open DevTools (F12)
2. Press Ctrl+Shift+Delete to open Clear Browsing Data
3. Clear "Cache" for "All time"
4. OR: Right-click the page and select "Hard refresh" (Ctrl+Shift+R)

## Step 4: Restart Dev Server

```bash
npm run dev
```

## Step 5: Open in Fresh Tab

- Open a new tab
- Go to: http://localhost:5173

---

## If Still Blank: Check Console

1. Open DevTools (F12)
2. Go to Console tab
3. Look for any red error messages
4. Take a screenshot and share the error

---

## Common Errors & Solutions

### Error: Cannot find module 'tailwindcss'
```bash
npm install tailwindcss postcss autoprefixer
```

### Error: React components not rendering
This is usually a Tailwind CSS issue. Try:
```bash
npm run build  # Test production build
```

### Error: Port 5173 already in use
Vite will automatically use port 5174, 5175, etc.
Check the console output for the actual URL.

---

## What I Fixed

I've made these changes to fix the blank page issue:

1. ✅ Fixed Tailwind CSS import syntax (v4 compatibility)
2. ✅ Added `tailwind.config.js` configuration
3. ✅ Added `postcss.config.js` configuration
4. ✅ Fixed Zustand store initialization in routes

## Try These Steps Now

```bash
cd frontend

# Option A: Quick fix
npm run dev

# Option B: Full clean install
rm -rf node_modules package-lock.json
npm install
npm run dev
```

Open: **http://localhost:5173**

You should now see the OnlineShop Home page with the navbar!

---

## Still Having Issues?

### Check if dependencies are installed
```bash
npm list react react-dom react-router
```

### Verify backend is running
```bash
docker compose ps
```

All services should show "Up"

### Check browser console (F12)
Look for JavaScript errors in the Console tab

### Try different port
If 5173 is in use:
```bash
npm run dev -- --port 3000
```

Then visit: http://localhost:3000

---

## Expected Result

When working correctly, you should see:
- OnlineShop navbar at the top
- Hero section with gradient background
- "Get Started" and "Sign In" buttons
- Feature highlights below
- Fully styled with blue colors

If you see plain HTML without styling, the issue is Tailwind CSS.
If you see styled content, everything is working!

---

Let me know if this fixes it! 🎉
