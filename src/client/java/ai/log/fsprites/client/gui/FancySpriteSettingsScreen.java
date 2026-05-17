package ai.log.fsprites.client.gui;

import ai.log.fsprites.client.sprite.*;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Settings screen for managing FancySprites.
 * Theme: Figure Mode (dark background, orange accents, sharp corners)
 */
public class FancySpriteSettingsScreen extends Screen {

    private static final int ENTRY_HEIGHT = 64;
    private static final int CATEGORY_HEIGHT = 22;
    private static final int PREVIEW_SIZE = 40;
    private static final int PADDING = 12;
    private static final int HEADER_H = 40;
    private static final int FOOTER_H = 36;
    private static final int SCROLLBAR_W = 6;
    private static final int BTN_W = 80;
    private static final int BTN_H = 20;
    private static final int SEARCH_W = 220;
    private static final int SEARCH_H = 20;

    private static final int TOGGLE_W = 36;
    private static final int TOGGLE_H = 16;

    private static final int COL_BG = 0xFF121212;
    private static final int COL_PANEL = 0xFF1E1E1E;
    private static final int COL_PANEL_ALT = 0xFF1A1A1A;
    private static final int COL_BORDER = 0xFF333333;
    private static final int COL_ACCENT = 0xFFFF6600;
    private static final int COL_ACCENT2 = 0xFFCC4400;
    private static final int COL_TEXT = 0xFFEEEEEE;
    private static final int COL_SUBTEXT = 0xFF888888;
    private static final int COL_PREVIEW_BG = 0xFF0A0A0A;

    private final Screen previousScreen;
    private final SpritePersistence persistence;
    private final SpriteManager spriteManager;
    private final SpriteTextureManager textureManager;

    private EditBox searchBox;
    private String searchQuery = "";

    private final List<DisplayRow> displayRows = new ArrayList<>();

    private int visibleSpriteCount = 0;
    private int visibleCategoryCount = 0;

    private int scrollY = 0;
    private int maxScrollY = 0;

    private boolean isDraggingScrollbar = false;
    private int scrollbarDragStartY = 0;
    private int scrollbarDragStartScroll = 0;

    private int contentTop;
    private int contentBottom;
    private int contentHeight;

    private static class SpriteEntry {
        final SpriteConfig config;
        final SpriteAnimationPlayer animationPlayer;

        SpriteEntry(SpriteConfig config, SpriteAnimationPlayer animationPlayer) {
            this.config = config;
            this.animationPlayer = animationPlayer;
        }
    }

    private enum RowType {
        CATEGORY,
        SPRITE
    }

    private static class DisplayRow {
        final RowType type;
        final String category;
        final SpriteEntry entry;
        final int height;
        final int itemCount;

        DisplayRow(RowType type, String category, SpriteEntry entry, int height, int itemCount) {
            this.type = type;
            this.category = category;
            this.entry = entry;
            this.height = height;
            this.itemCount = itemCount;
        }
    }

    public FancySpriteSettingsScreen(Screen previousScreen) {
        super(Component.literal("FancySprites Settings"));
        this.previousScreen = previousScreen;
        this.persistence = SpritePersistence.getInstance();
        this.spriteManager = SpriteManager.getInstance();
        this.textureManager = SpriteTextureManager.getInstance();
    }

    @Override
    protected void init() {
        super.init();

        this.scrollY = 0;
        contentTop = HEADER_H;
        contentBottom = this.height - FOOTER_H;
        contentHeight = contentBottom - contentTop;

        int searchX = this.width - PADDING - SEARCH_W;
        int searchY = 10;
        this.searchBox = this.addRenderableWidget(new EditBox(this.font, searchX, searchY, SEARCH_W, SEARCH_H, Component.literal("Search sprites")));
        this.searchBox.setValue(this.searchQuery);
        this.setFocused(this.searchBox);
        this.searchBox.setResponder(value -> {
            this.searchQuery = value != null ? value : "";
            rebuildRows();
        });

        rebuildRows();
    }

    private void rebuildRows() {
        this.displayRows.clear();
        this.visibleSpriteCount = 0;
        this.visibleCategoryCount = 0;

        List<SpriteConfig> sprites = new ArrayList<>(spriteManager.getAllSprites());
        sprites.sort(Comparator
                .comparing((SpriteConfig sprite) -> normalize(sprite.category))
                .thenComparingInt(sprite -> sprite.zIndex)
                .thenComparing(sprite -> sprite.id));

        Map<String, List<SpriteEntry>> grouped = new LinkedHashMap<>();
        for (SpriteConfig sprite : sprites) {
            if (!matchesSearch(sprite, searchQuery)) {
                continue;
            }

            SpriteAnimationPlayer player = sprite.animation != null
                    ? new SpriteAnimationPlayer(sprite)
                    : null;

            grouped.computeIfAbsent(sprite.category, key -> new ArrayList<>())
                    .add(new SpriteEntry(sprite, player));
        }

        int totalHeight = 0;
        for (Map.Entry<String, List<SpriteEntry>> entry : grouped.entrySet()) {
            String category = entry.getKey();
            List<SpriteEntry> categorySprites = entry.getValue();
            if (categorySprites.isEmpty()) {
                continue;
            }

            categorySprites.sort(Comparator
                    .comparingInt((SpriteEntry spriteEntry) -> spriteEntry.config.zIndex)
                    .thenComparing(spriteEntry -> spriteEntry.config.id));

            displayRows.add(new DisplayRow(RowType.CATEGORY, category, null, CATEGORY_HEIGHT, categorySprites.size()));
            totalHeight += CATEGORY_HEIGHT;

            for (SpriteEntry spriteEntry : categorySprites) {
                displayRows.add(new DisplayRow(RowType.SPRITE, category, spriteEntry, ENTRY_HEIGHT, 0));
                totalHeight += ENTRY_HEIGHT;
                visibleSpriteCount++;
            }

            visibleCategoryCount++;
        }

        maxScrollY = Math.max(0, totalHeight - contentHeight);
        scrollY = Math.min(scrollY, maxScrollY);
    }

    private static String normalize(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT);
    }

    private boolean matchesSearch(SpriteConfig sprite, String query) {
        if (query == null || query.isBlank()) {
            return true;
        }

        String haystack = normalize(sprite.id) + " " + normalize(sprite.description) + " " + normalize(sprite.category);

        for (String token : normalize(query).split("\\s+")) {
            if (token.isBlank()) {
                continue;
            }
            if (!haystack.contains(token)) {
                return false;
            }
        }

        return true;
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        g.fill(0, 0, this.width, this.height, COL_BG);

        g.fill(0, 0, this.width, HEADER_H, COL_PANEL);
        g.fill(0, HEADER_H - 2, this.width, HEADER_H, COL_ACCENT);

        g.drawString(this.font, "✦ FancySprites Menu", PADDING, (HEADER_H - 8) / 2, COL_ACCENT, false);

        if (searchBox != null) {
            searchBox.render(g, mouseX, mouseY, partialTick);
        }

        String badge = visibleSpriteCount + " sprites / " + visibleCategoryCount + " groups";
        int badgeW = this.font.width(badge) + 10;
        int badgeX = this.width - PADDING - SEARCH_W - PADDING - badgeW;

        g.fill(badgeX - 2, 10, badgeX + badgeW, HEADER_H - 10, COL_ACCENT2);
        g.drawString(this.font, badge, badgeX + 3, (HEADER_H - 8) / 2, COL_TEXT, false);

        g.fill(0, contentBottom, this.width, this.height, COL_PANEL);
        g.fill(0, contentBottom, this.width, contentBottom + 2, COL_ACCENT);

        renderRows(g);

        if (maxScrollY > 0) {
            renderScrollbar(g);
        }

        renderBackButton(g, mouseX, mouseY);

        if (displayRows.isEmpty()) {
            String msg = searchQuery == null || searchQuery.isBlank()
                    ? "No sprites loaded"
                    : "No sprites match search";

            g.drawString(
                    this.font,
                    msg,
                    (this.width - this.font.width(msg)) / 2,
                    contentTop + (contentHeight - 8) / 2,
                    COL_SUBTEXT,
                    false
            );
        }
    }

    private void renderRows(GuiGraphics g) {
        g.enableScissor(0, contentTop, this.width, contentBottom);

        int currentY = contentTop - scrollY;
        int rowIndex = 0;

        for (DisplayRow row : displayRows) {
            if (currentY + row.height <= contentTop) {
                currentY += row.height;
                rowIndex++;
                continue;
            }

            if (currentY >= contentBottom) {
                break;
            }

            if (row.type == RowType.CATEGORY) {
                renderCategoryHeader(g, row, currentY);
            } else {
                renderSpriteRow(g, row, currentY, rowIndex);
            }

            currentY += row.height;
            rowIndex++;
        }

        g.disableScissor();
    }

    private void renderCategoryHeader(GuiGraphics g, DisplayRow row, int y) {
        int yTop = Math.max(y, contentTop);
        int yBot = Math.min(y + row.height, contentBottom);

        g.fill(0, yTop, this.width - SCROLLBAR_W - 2, yBot, COL_PANEL_ALT);
        g.fill(0, yTop, 3, yBot, COL_ACCENT2);

        String label = row.category;
        g.drawString(this.font, label, PADDING, yTop + 7, COL_ACCENT, false);

        String count = row.itemCount + " items";
        int countW = this.font.width(count);
        g.drawString(this.font, count, this.width - SCROLLBAR_W - PADDING - countW, yTop + 7, COL_SUBTEXT, false);
    }

    private void renderSpriteRow(GuiGraphics g, DisplayRow row, int y, int rowIndex) {
        SpriteEntry entry = row.entry;

        int yTop = Math.max(y, contentTop);
        int yBot = Math.min(y + row.height, contentBottom);

        g.fill(0, yTop, this.width - SCROLLBAR_W - 2, yBot, (rowIndex % 2 == 0) ? COL_PANEL : COL_PANEL_ALT);
        if (y + row.height <= contentBottom) {
            g.fill(PADDING, y + row.height - 1, this.width - SCROLLBAR_W - PADDING, y + row.height, COL_BORDER);
        }
        g.fill(0, yTop, 3, yBot, (rowIndex % 2 == 0) ? COL_ACCENT : COL_ACCENT2);

        if (entry.animationPlayer != null) {
            entry.animationPlayer.update();
        }

        renderSpritePreview(g, entry.config, PADDING + 3, y + (ENTRY_HEIGHT - PREVIEW_SIZE) / 2, PREVIEW_SIZE, PREVIEW_SIZE);

        boolean enabled = persistence.isEnabled(entry.config.id);
        int toggleX = PADDING + PREVIEW_SIZE + PADDING;
        int toggleY = y + (ENTRY_HEIGHT - TOGGLE_H) / 2;
        renderToggle(g, toggleX, toggleY, enabled);

        int textX = toggleX + TOGGLE_W + 14;
        int textY = y + (ENTRY_HEIGHT / 2 - (entry.config.description != null && !entry.config.description.isEmpty() ? 9 : 4));
        int maxTextWidth = this.width - textX - SCROLLBAR_W - PADDING * 2;

        String title = ellipsize(entry.config.id, maxTextWidth);
        g.drawString(this.font, title, textX, textY, COL_ACCENT, false);

        if (entry.config.description != null && !entry.config.description.isEmpty()) {
            String desc = ellipsize(entry.config.description, maxTextWidth);
            g.drawString(this.font, desc, textX, textY + 12, COL_SUBTEXT, false);
        }

        String category = ellipsize(entry.config.category, maxTextWidth);
        g.drawString(this.font, category, textX, textY + 24, COL_SUBTEXT, false);
    }

    private void renderToggle(GuiGraphics g, int x, int y, boolean enabled) {
        int bg = enabled ? COL_ACCENT : COL_BORDER;

        g.fill(x, y, x + TOGGLE_W, y + TOGGLE_H, bg);
        g.fill(x, y, x + 2, y + TOGGLE_H, enabled ? 0xFFFFAA00 : COL_SUBTEXT);

        int knobSize = TOGGLE_H - 4;
        int knobX = enabled ? x + TOGGLE_W - knobSize - 2 : x + 2;

        g.fill(knobX, y + 2, knobX + knobSize, y + 2 + knobSize, COL_TEXT);
    }

    private void renderSpritePreview(GuiGraphics g, SpriteConfig config, int slotX, int slotY, int slotW, int slotH) {
        g.fill(slotX, slotY, slotX + slotW, slotY + slotH, COL_PREVIEW_BG);

        int frameIndex = getPreviewFrameIndex(config);
        Identifier texture = textureManager.getFrameTexture(config.id, frameIndex);
        int[] size = textureManager.getFrameSize(config.id, frameIndex);

        if (texture != null && size != null && size.length >= 2 && size[0] > 0 && size[1] > 0) {
            float scale = Math.min((float) slotW / size[0], (float) slotH / size[1]);
            int drawW = Math.max(1, Math.round(size[0] * scale));
            int drawH = Math.max(1, Math.round(size[1] * scale));
            int drawX = slotX + (slotW - drawW) / 2;
            int drawY = slotY + (slotH - drawH) / 2;

            g.blit(RenderPipelines.GUI_TEXTURED, texture, drawX, drawY, 0.0f, 0.0f, drawW, drawH, drawW, drawH, 0xFFFFFFFF);
        } else {
            int cx = slotX + slotW / 2;
            int cy = slotY + slotH / 2;

            g.fill(cx - 1, slotY + 4, cx + 1, slotY + slotH - 4, COL_ACCENT2);
            g.fill(slotX + 4, cy - 1, slotX + slotW - 4, cy + 1, COL_ACCENT2);
        }

        g.fill(slotX, slotY, slotX + slotW, slotY + 1, COL_ACCENT);
        g.fill(slotX, slotY + slotH - 1, slotX + slotW, slotY + slotH, COL_ACCENT);
        g.fill(slotX, slotY, slotX + 1, slotY + slotH, COL_ACCENT);
        g.fill(slotX + slotW - 1, slotY, slotX + slotW, slotY + slotH, COL_ACCENT);
    }

    private int getPreviewFrameIndex(SpriteConfig config) {
        if (config.animation == null || config.animation.frames == null || config.animation.frames.isEmpty()) {
            return 0;
        }

        AnimationFrame first = config.animation.getFrame(0);
        return first != null ? first.index : 0;
    }

    private void renderBackButton(GuiGraphics g, int mouseX, int mouseY) {
        int bx = this.width - BTN_W - PADDING;
        int by = this.height - FOOTER_H + (FOOTER_H - BTN_H) / 2;

        boolean hovered = mouseX >= bx && mouseX <= bx + BTN_W && mouseY >= by && mouseY <= by + BTN_H;

        g.fill(bx, by, bx + BTN_W, by + BTN_H, hovered ? COL_ACCENT : COL_ACCENT2);
        g.fill(bx, by, bx + 2, by + BTN_H, hovered ? 0xFFFFAA00 : COL_ACCENT);

        String label = "← Back";
        int textW = this.font.width(label);

        g.drawString(this.font, label, bx + (BTN_W - textW) / 2, by + (BTN_H - 8) / 2, COL_TEXT, false);
    }

    private void renderScrollbar(GuiGraphics g) {
        int tx = scrollbarX();
        int tt = scrollbarTrackTop();
        int th = scrollbarTrackHeight();

        g.fill(tx, tt, tx + SCROLLBAR_W, tt + th, COL_BORDER);

        int thumbH = scrollbarThumbHeight();
        int thumbT = scrollbarThumbTop();

        g.fill(tx, thumbT, tx + SCROLLBAR_W, thumbT + thumbH, COL_ACCENT2);
        g.fill(tx, thumbT, tx + 2, thumbT + thumbH, COL_ACCENT);
    }

    private int scrollbarX() {
        return this.width - SCROLLBAR_W - 2;
    }

    private int scrollbarTrackTop() {
        return contentTop + 2;
    }

    private int scrollbarTrackHeight() {
        return contentHeight - 4;
    }

    private int totalContentHeight() {
        int total = 0;
        for (DisplayRow row : displayRows) {
            total += row.height;
        }
        return total;
    }

    private int scrollbarThumbHeight() {
        int totalH = totalContentHeight();
        if (totalH <= 0) {
            return scrollbarTrackHeight();
        }

        return Math.max(16, Math.round(scrollbarTrackHeight() * (float) contentHeight / totalH));
    }

    private int scrollbarThumbTop() {
        if (maxScrollY == 0) {
            return scrollbarTrackTop();
        }

        return scrollbarTrackTop() + Math.round((scrollbarTrackHeight() - scrollbarThumbHeight()) * (float) scrollY / maxScrollY);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean consumed) {
        if (!consumed) {
            if (searchBox != null && searchBox.mouseClicked(event, consumed)) {
                return true;
            }

            if (event.button() == 0) {
                double mx = event.x();
                double my = event.y();

                int bx = this.width - BTN_W - PADDING;
                int by = this.height - FOOTER_H + (FOOTER_H - BTN_H) / 2;
                if (mx >= bx && mx <= bx + BTN_W && my >= by && my <= by + BTN_H) {
                    this.onClose();
                    return true;
                }

                int currentY = contentTop - scrollY;
                for (DisplayRow row : displayRows) {
                    if (currentY + row.height <= contentTop) {
                        currentY += row.height;
                        continue;
                    }
                    if (currentY >= contentBottom) {
                        break;
                    }

                    if (row.type == RowType.SPRITE) {
                        int tx = PADDING + PREVIEW_SIZE + PADDING;
                        int ty = currentY + (ENTRY_HEIGHT - TOGGLE_H) / 2;

                        if (mx >= tx && mx <= tx + TOGGLE_W && my >= ty && my <= ty + TOGGLE_H) {
                            boolean current = persistence.isEnabled(row.entry.config.id);
                            persistence.setEnabled(row.entry.config.id, !current);
                            return true;
                        }
                    }

                    currentY += row.height;
                }

                if (maxScrollY > 0) {
                    int sx = scrollbarX();
                    if (mx >= sx && mx <= sx + SCROLLBAR_W && my >= scrollbarThumbTop() && my <= scrollbarThumbTop() + scrollbarThumbHeight()) {
                        isDraggingScrollbar = true;
                        scrollbarDragStartY = (int) my;
                        scrollbarDragStartScroll = scrollY;
                        return true;
                    }
                }
            }
        }

        return super.mouseClicked(event, consumed);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (isDraggingScrollbar && event.button() == 0) {
            isDraggingScrollbar = false;
            return true;
        }

        return super.mouseReleased(event);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        if (isDraggingScrollbar && event.button() == 0) {
            int delta = (int) event.y() - scrollbarDragStartY;
            int trackH = scrollbarTrackHeight();
            int thumbH = scrollbarThumbHeight();

            if (trackH > thumbH) {
                float ratio = (float) maxScrollY / (trackH - thumbH);
                scrollY = Math.max(0, Math.min(maxScrollY, scrollbarDragStartScroll + Math.round(delta * ratio)));
            }

            return true;
        }

        return super.mouseDragged(event, dragX, dragY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollXAmount, double scrollYAmount) {
        if (mouseY >= contentTop && mouseY <= contentBottom) {
            scrollBy((int) (-scrollYAmount * 16));
            return true;
        }

        return super.mouseScrolled(mouseX, mouseY, scrollXAmount, scrollYAmount);
    }

    @Override
    public boolean charTyped(CharacterEvent event) {
        if (searchBox != null && searchBox.charTyped(event)) {
            return true;
        }

        return super.charTyped(event);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (searchBox != null && searchBox.keyPressed(event)) {
            return true;
        }

        return super.keyPressed(event);
    }

    private void scrollBy(int delta) {
        scrollY = Math.max(0, Math.min(maxScrollY, scrollY + delta));
    }

    private String ellipsize(String text, int maxWidth) {
        if (this.font.width(text) <= maxWidth) {
            return text;
        }

        String dots = "...";
        while (text.length() > 0 && this.font.width(text + dots) > maxWidth) {
            text = text.substring(0, text.length() - 1);
        }

        return text + dots;
    }

    @Override
    public void onClose() {
        this.minecraft.setScreen(previousScreen);
    }

    @Override
    public boolean isPauseScreen() {
        return true;
    }
}
