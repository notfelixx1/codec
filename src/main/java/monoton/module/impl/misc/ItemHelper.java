package monoton.module.impl.misc;

import com.mojang.blaze3d.matrix.MatrixStack;
import monoton.control.events.client.Event;
import monoton.control.events.render.EventContainerRender;
import monoton.control.events.render.EventCooldown;
import monoton.control.events.render.EventHotbarRender;
import monoton.control.events.render.EventRenderTooltip;
import monoton.module.TypeList;
import monoton.module.api.Annotation;
import monoton.module.api.Module;
import monoton.module.impl.render.ShulkerPreview;
import monoton.module.settings.imp.*;
import monoton.utils.other.OtherUtil;
import monoton.utils.render.RenderUtilka;
import monoton.utils.render.shader.AnimationUtil;
import monoton.utils.render.shader.ColorUtil;
import monoton.utils.world.InventoryUtils;
import net.minecraft.client.gui.screen.inventory.CreativeScreen;
import net.minecraft.inventory.container.PlayerContainer;
import net.minecraft.inventory.container.Slot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.*;
import net.minecraft.potion.Effects;
import net.minecraft.util.text.IFormattableTextComponent;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TextFormatting;

import java.util.List;

@Annotation(name = "ItemHelper", type = TypeList.Misc, desc = "Авто свап предметов левой руки")
public class ItemHelper extends Module {
    private final BindSetting chorus = new BindSetting("Хорус", -1);
    private final BindSetting golden_apple = new BindSetting("Золотое яблоко", -1);
    private final BindSetting enchant_golden_apple = new BindSetting("Чарка", -1);
    private final BindSetting trident = new BindSetting("Трезубец", -1);
    private final BindSetting exp = new BindSetting("Пузырек опыта", -1);
    private final BindSetting shield = new BindSetting("Щит", -1);
    private final BindSetting instant_health = new BindSetting("Зелье исцеления", -1);

    private final InfoSetting health = new InfoSetting("Здоровье",()-> {});

    private final BooleanOption show_instant_health = new BooleanOption("Подсвет зелье исцеления", false);
    private final ColorSetting color_instant_health = new ColorSetting("Цвет зелья исцеления",  ColorUtil.hex("#FF2AB9")).setVisible(show_instant_health::get);
    private final BooleanOption show_enchant_golden_apple = new BooleanOption("Подсвет чарки", false);
    private final ColorSetting color_enchant_golden_apple = new ColorSetting("Цвет чарки",  ColorUtil.hex("#FFAC93")).setVisible(show_enchant_golden_apple::get);
    private final BooleanOption show_golden_apple = new BooleanOption("Подсвет золотые яблоки", false);
    private final ColorSetting color_golden_apple = new ColorSetting("Цвет золотого яблока",  ColorUtil.hex("#E7EB56")).setVisible(show_golden_apple::get);

    private final InfoSetting other = new InfoSetting("Остальное",()-> {});

    private final BooleanOption decreaseCooldown = new BooleanOption("Уменьшать кд предметов", false);
    private final BooleanOption show_new_items = new BooleanOption("Подсвечивать только что поднятые предметы", false);
    private final BooleanOption show_nbt = new BooleanOption("Показывать nbt предметов", false);

    private final AnimationUtil anim = new AnimationUtil(0.0f, 4);

    public ItemHelper() {
        addSettings(health, show_instant_health, color_instant_health, show_enchant_golden_apple, color_enchant_golden_apple, show_golden_apple, color_golden_apple, other, decreaseCooldown, show_nbt);
    }

    @Override
    public boolean onEvent(Event event) {
        if (event instanceof EventCooldown e) {
            onCooldown(e);
        }

        if (event instanceof EventContainerRender.Pre e) {
            onContainerRenderPre(e);
        }

        if (event instanceof EventHotbarRender.Pre e) {
            onHotbarRender(e);
        }

        if (event instanceof EventRenderTooltip e) {
            onRenderTooltip(e);
        }
        return false;
    }

    /*
    @EventTarget
    public void onKey(EventKey e) {
        if (mc.player == null || !e.isHold()) return;

        if (chorus.get() == e.getKey()) {
            InventoryUtil.useItemLegit(Items.CHORUS_FRUIT);
        }

        if (golden_apple.get() == e.getKey()) {
            InventoryUtil.useItemLegit(Items.GOLDEN_APPLE);
        }

        if (enchant_golden_apple.get() == e.getKey()) {
            InventoryUtil.useItemLegit(Items.ENCHANTED_GOLDEN_APPLE);
        }

        if (trident.get() == e.getKey()) {
            InventoryUtil.useItemLegit(Items.TRIDENT);
        }

        if (exp.get() == e.getKey()) {
            InventoryUtil.useItemLegit(Items.EXPERIENCE_BOTTLE);
        }

        if (shield.get() == e.getKey()) {
            InventoryUtil.useItemLegit(Items.SHIELD);
        }

        if (instant_health.get() == e.getKey()) {
            int slot = InventoryUtil.findPotionSlotWithEffects(true, true, true, true, Effects.INSTANT_HEALTH);
            if (slot != -1) InventoryUtil.useItemLegit(Item.getItemById(slot));
        }
    }

     */


    public void onCooldown(EventCooldown e) {
        Item item = e.getItem();
        ItemStack itemStack = new ItemStack(item);
        if (decreaseCooldown.get() && itemStack.isFood()) {
            int reduction = item.getFood().isFastEating() ? 16 : 32;
            int originalTicks = e.getTicks();

            if (originalTicks > reduction && itemStack.getItem() != Items.DRIED_KELP) {
                e.setTicks(originalTicks - reduction);
                IFormattableTextComponent message = new StringTextComponent("Задержка на ").append(new StringTextComponent(item.getName().getString())).append(new StringTextComponent(" уменьшена на ~" + (reduction / 20.0) + " секунды")).setStyle(new StringTextComponent("").getStyle().applyFormatting(TextFormatting.GRAY));
                OtherUtil.sendMessage(message);
            }
        }
    }

    public void onContainerRenderPre(EventContainerRender.Pre e) {
        if (mc.currentScreen instanceof CreativeScreen || !(e.getContainer() instanceof PlayerContainer)) return;

        for (Slot slot : e.getContainer().inventorySlots) {
            if (slot == null || !slot.getHasStack()) continue;
            renderItemHighlight(e.getStack(), slot.getStack(), e.getGuiLeft() + slot.xPos, e.getGuiTop() + slot.yPos);
        }
    }

    public void onHotbarRender(EventHotbarRender.Pre e) {
        if (mc.currentScreen instanceof CreativeScreen) return;

        int centerX = mc.getMainWindow().getScaledWidth() / 2;
        int baseY = mc.getMainWindow().getScaledHeight() - 16 - 3;

        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.player.inventory.mainInventory.get(i);
            if (stack.isEmpty()) continue;
            renderItemHighlight(e.getStack(), stack, centerX - 90 + i * 20 + 2, baseY);
        }
    }

    private void renderItemHighlight(MatrixStack matrixStack, ItemStack stack, float x, float y) {
        int color = getItemHighlightColor(stack);
        if (color != 0) {
            float norm = (anim.getValue() + 0.75f) / 1.5f;
            int alpha = (int) (75 + norm * 100);
            RenderUtilka.Render2D.drawMinecraftRectangle(matrixStack, x, y, 16, 16, ColorUtil.applyOpacity(color, alpha));
        }
    }

    private int getItemHighlightColor(ItemStack stack) {
        if (show_enchant_golden_apple.get() && stack.getItem() == Items.ENCHANTED_GOLDEN_APPLE) {
            return color_enchant_golden_apple.get();
        }
        if (show_golden_apple.get() && stack.getItem() == Items.GOLDEN_APPLE) {
            return color_golden_apple.get();
        }
        if (show_instant_health.get() && InventoryUtils.stackHasAnyEffect(stack, true, false, false, Effects.INSTANT_HEALTH)) {
            return color_instant_health.get();
        }
        return 0;
    }

    public void onRenderTooltip(EventRenderTooltip e) {
        if (ShulkerPreview.shouldShowPreview(e.stack)) return;
        if (!show_nbt.get() || !e.stack.hasTag()) return;
        e.setCancel(true);

        List<ITextComponent> tooltip = mc.currentScreen.getTooltipFromItem(e.stack);
        tooltip.add(StringTextComponent.EMPTY);
        addTag(tooltip, e.stack.getTag(), 0);

        mc.currentScreen.func_243308_b(e.matrixStack, tooltip, e.mouseX, e.mouseY);
    }

    private void addTag(List<ITextComponent> tooltip, CompoundNBT tag, int depth) {
        String indent = "  ".repeat(depth);
        for (String key : tag.keySet()) {
            INBT base = tag.get(key);
            String type = getNbtType(base);
            tooltip.add(new StringTextComponent(indent + "- " + type + ": " + key).mergeStyle(TextFormatting.DARK_GRAY));
        }
    }

    private String getNbtType(INBT nbt) {
        if (nbt instanceof ByteNBT) return "byte";
        if (nbt instanceof ShortNBT) return "short";
        if (nbt instanceof IntNBT) return "int";
        if (nbt instanceof LongNBT) return "long";
        if (nbt instanceof FloatNBT) return "float";
        if (nbt instanceof DoubleNBT) return "double";
        if (nbt instanceof StringNBT) return "string";
        if (nbt instanceof ByteArrayNBT) return "byte[]";
        if (nbt instanceof IntArrayNBT) return "int[]";
        if (nbt instanceof LongArrayNBT) return "long[]";
        if (nbt instanceof ListNBT) return "list";
        if (nbt instanceof CompoundNBT) return "compound";
        return "unknown";
    }
}