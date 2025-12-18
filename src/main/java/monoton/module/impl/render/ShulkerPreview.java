package monoton.module.impl.render;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;
import monoton.control.events.client.Event;
import monoton.control.events.render.EventRender;
import monoton.control.events.render.EventRenderTooltip;
import monoton.module.api.Annotation;
import monoton.module.api.Module;
import monoton.module.settings.imp.BooleanOption;
import monoton.utils.math.MathUtil;
import monoton.utils.render.RenderUtilka;
import monoton.utils.render.shader.ProjectUtil;
import net.minecraft.block.ShulkerBoxBlock;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.ItemEntity;
import net.minecraft.inventory.ItemStackHelper;
import net.minecraft.item.BlockItem;
import net.minecraft.item.DyeColor;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.IReorderingProcessor;
import net.minecraft.util.NonNullList;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TextFormatting;
import org.joml.Vector2f;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import static monoton.module.TypeList.Render;

@Annotation(name = "ShulkerPreview", type = Render, desc = "Превью содержимого шалкера")
public class ShulkerPreview extends Module {

    private final BooleanOption world = new BooleanOption("Отображать в мире", true);

    public ShulkerPreview() {
        addSettings(world);
    }

    @Override
    public boolean onEvent(Event event) {
        if (event instanceof EventRenderTooltip e) {
            ItemStack stack = e.stack;
            if (shouldShowPreview(stack)) {
                e.setCancel(true);
                showPreview(e.matrixStack, stack, e.mouseX, e.mouseY);
            }
            return false;
        }

        if (event instanceof EventRender e && world.get() && mc.world != null && mc.player != null) {
            List<ItemEntity> shulkerDrops = new ArrayList<>();

            for (Entity ent : mc.world.getAllEntities()) {
                if (ent instanceof ItemEntity itemEnt) {
                    ItemStack drop = itemEnt.getItem();
                    if (drop.getItem() instanceof BlockItem blockItem
                            && blockItem.getBlock() instanceof ShulkerBoxBlock) {
                        shulkerDrops.add(itemEnt);
                    }
                }
            }

            shulkerDrops.sort(Comparator.comparingDouble(
                    (ItemEntity ie) -> ie.getDistanceSq(mc.player.getPositionVec())
            ).reversed());

            for (ItemEntity itemEnt : shulkerDrops) {
                ItemStack drop = itemEnt.getItem();
                CompoundNBT rootTag = drop.getTag();
                if (rootTag == null || !rootTag.contains("BlockEntityTag")) continue;

                Vector2f screen = ProjectUtil.project2D(MathUtil.interpolate(itemEnt, e.getPartialTicks()));
                if (screen.x == Float.MAX_VALUE || screen.y == Float.MAX_VALUE) continue;

                float panelX = screen.x + 14;
                float panelY = screen.y - 20;

                DyeColor dropColor = ShulkerBoxBlock.getColorFromItem(drop.getItem());
                int tint = 0xFF000000 | (dropColor != null ? dropColor.getColorValue() : DyeColor.PURPLE.getColorValue());

                RenderUtilka.drawImage(
                        new ResourceLocation("monoton/images/shulker_box_tooltip.png"),
                        panelX, panelY, 128, 128, tint
                );

                CompoundNBT blockTag = rootTag.getCompound("BlockEntityTag");
                NonNullList<ItemStack> items = NonNullList.withSize(27, ItemStack.EMPTY);
                ItemStackHelper.loadAllItems(blockTag, items);

                for (int i = 0; i < items.size(); i++) {
                    ItemStack content = items.get(i);
                    if (content.isEmpty()) continue;

                    int col = i % 9;
                    int row = i / 9;
                    float slotX = panelX + 4 + col * 9;
                    float slotY = panelY + 4 + row * 9;

                    RenderUtilka.drawStack(content, slotX, slotY, 0.5f);
                }
            }
        }
        return false;
    }

    public static boolean shouldShowPreview(ItemStack stack) {
        return Screen.hasControlDown()
                && stack.getItem() instanceof BlockItem blockItem
                && blockItem.getBlock() instanceof ShulkerBoxBlock;
    }

    public static void showPreview(MatrixStack matrixStack, ItemStack stack, int mouseX, int mouseY) {
        List<ITextComponent> texts = new ArrayList<>();
        texts.add(stack.getDisplayName().deepCopy().mergeStyle(TextFormatting.WHITE));
        texts.add(new StringTextComponent("Содержит 27 предметов").mergeStyle(TextFormatting.GRAY));

        List<IReorderingProcessor> procs = texts.stream()
                .map(ITextComponent::func_241878_f)
                .collect(Collectors.toList());

        mc.currentScreen.renderTooltip(matrixStack, procs, mouseX, mouseY);

        int maxWidth = procs.stream()
                .mapToInt(mc.fontRenderer::func_243245_a)
                .max().orElse(0);

        int x = mouseX + 12;
        int y = mouseY - 12;
        int height = 8;
        if (procs.size() > 1) {
            height += 2 + (procs.size() - 1) * 10;
        }

        if (x + maxWidth > mc.currentScreen.width) {
            x -= 28 + maxWidth;
        }
        if (y + height + 6 > mc.currentScreen.height) {
            y = mc.currentScreen.height - height - 6;
        }

        DyeColor dye = ShulkerBoxBlock.getColorFromItem(stack.getItem());
        int tint = 0xFF000000 | (dye != null ? dye.getColorValue() : DyeColor.PURPLE.getColorValue());

        RenderUtilka.drawImage(
                new ResourceLocation("monoton/images/shulker_box_tooltip.png"),
                x - 4, y + height + 5, 255, 255, tint
        );

        CompoundNBT rootTag = stack.getTag();
        if (rootTag != null && rootTag.contains("BlockEntityTag")) {
            CompoundNBT blockTag = rootTag.getCompound("BlockEntityTag");
            NonNullList<ItemStack> items = NonNullList.withSize(27, ItemStack.EMPTY);
            ItemStackHelper.loadAllItems(blockTag, items);

            int startX = x + 4;
            int startY = y + height + 13;

            RenderSystem.pushMatrix();
            RenderSystem.translatef(0.0F, 0.0F, 32.0F);
            mc.currentScreen.setBlitOffset(200);
            mc.getItemRenderer().zLevel = 200.0F;

            for (int i = 0; i < items.size(); i++) {
                ItemStack content = items.get(i);
                if (content.isEmpty()) continue;

                int col = i % 9;
                int row = i / 9;
                int slotX = startX + col * 18;
                int slotY = startY + row * 18;

                RenderUtilka.drawStack(content, slotX, slotY, 1.0f);
            }

            mc.currentScreen.setBlitOffset(0);
            mc.getItemRenderer().zLevel = 0.0F;
            RenderSystem.popMatrix();
        }
    }
}