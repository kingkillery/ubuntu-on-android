const express = require('express');
const puppeteer = require('puppeteer');
const fs = require('fs');
const app = express();
const PORT = 3000;

app.use(express.json());

let browser;
let page;
let isPaused = false;

// Initialize Browser
async function initBrowser() {
    try {
        console.log('Launching browser...');
        browser = await puppeteer.launch({
            headless: true,
            args: ['--no-sandbox', '--disable-setuid-sandbox']
        });
        page = await browser.newPage();
        await page.setViewport({ width: 1280, height: 720 });
        await page.goto('https://www.google.com'); // Default start page
        console.log('Browser launched successfully');
    } catch (error) {
        console.error('Failed to launch browser:', error);
    }
}

// Ensure browser is running
app.use(async (req, res, next) => {
    if (!browser) await initBrowser();
    next();
});

// GET /screenshot - Returns current viewport
app.get('/screenshot', async (req, res) => {
    if (!page) return res.status(503).send('Browser not ready');
    try {
        const imageBuffer = await page.screenshot({ type: 'jpeg', quality: 50 });
        res.set('Content-Type', 'image/jpeg');
        res.send(imageBuffer);
    } catch (error) {
        console.error('Screenshot failed:', error);
        res.status(500).send('Screenshot failed');
    }
});

// POST /control - Automation actions
app.post('/control', async (req, res) => {
    if (!page) return res.status(503).send('Browser not ready');
    const { action, payload } = req.body;

    console.log(`Received action: ${action}`, payload);

    if (isPaused && action !== 'RESUME' && action !== 'STATUS') {
        return res.status(400).json({ status: 'PAUSED', message: 'Automation is paused' });
    }

    try {
        let result = {};
        switch (action) {
            case 'NAVIGATE':
                await page.goto(payload.url);
                result = { message: `Navigated to ${payload.url}` };
                break;
            case 'CLICK':
                await page.mouse.click(payload.x, payload.y);
                result = { message: `Clicked at ${payload.x}, ${payload.y}` };
                break;
            case 'TYPE':
                await page.keyboard.type(payload.text);
                result = { message: `Typed "${payload.text}"` };
                break;
            case 'PAUSE':
                isPaused = true;
                result = { status: 'PAUSED' };
                break;
            case 'RESUME':
                isPaused = false;
                result = { status: 'RUNNING' };
                break;
            case 'STATUS':
                result = { 
                    status: isPaused ? 'PAUSED' : 'RUNNING',
                    url: page.url() 
                };
                break;
            default:
                return res.status(400).send('Unknown action');
        }
        res.json(result);
    } catch (error) {
        console.error(`Action ${action} failed:`, error);
        res.status(500).json({ error: error.message });
    }
});

// Start Server
app.listen(PORT, async () => {
    console.log(`Agent Browser Server running on port ${PORT}`);
    await initBrowser();
});
