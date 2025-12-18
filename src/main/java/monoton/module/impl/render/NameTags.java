package monoton.module.impl.render;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;
import mods.voicechat.voice.client.ClientManager;
import mods.voicechat.voice.client.ClientPlayerStateManager;
import mods.voicechat.voice.client.ClientVoicechat;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import monoton.control.Manager;
import monoton.control.events.client.Event;
import monoton.control.events.render.EventRender;
import monoton.module.TypeList;
import monoton.module.api.Annotation;
import monoton.module.api.Module;
import monoton.module.settings.imp.BooleanOption;
import monoton.module.settings.imp.MultiBoxSetting;
import monoton.module.settings.imp.SliderSetting;
import monoton.utils.IMinecraft;
import monoton.utils.font.Fonts;
import monoton.utils.math.MathUtil;
import monoton.utils.math.PlayerPositionTracker;
import monoton.utils.render.RenderUtilka;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.settings.PointOfView;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.*;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.scoreboard.Score;
import net.minecraft.scoreboard.ScoreObjective;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TextComponent;
import net.minecraft.util.text.TextFormatting;
import org.joml.Vector4d;
import org.lwjgl.opengl.GL11;

import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static monoton.utils.render.ColorUtils.rgba;

@Annotation(name = "NameTags", type = TypeList.Render, desc = "Показывает информацию игрока")
public class NameTags extends Module {

    public MultiBoxSetting elements = new MultiBoxSetting("Настройки",
            new BooleanOption("Броня", true),
            new BooleanOption("Зачарование", true),
            new BooleanOption("Показывать шары", true),
            new BooleanOption("Отображать VoiceChat", true));

    public SliderSetting size = new SliderSetting("Размер шрифта", 0.6f, 0.5f, 0.7f, 0.02f);

    public NameTags() {
        addSettings(elements, size);
    }

    public Object2ObjectOpenHashMap<Vector4d, PlayerEntity> positions = new Object2ObjectOpenHashMap<>();

    @Override
    public boolean onEvent(Event event) {
        if (event instanceof EventRender render) {
            if (render.isRender3D()) {
                updatePlayerPositions(render.partialTicks);
            }

            if (render.isRender2D()) {
                renderPlayerElements(render.matrixStack);
            }
        }
        return false;
    }

    private void updatePlayerPositions(float partialTicks) {
        this.positions.clear();
        Iterator var2 = mc.world.getPlayers().iterator();

        while (true) {
            PlayerEntity player;
            do {
                do {
                    do {
                        if (!var2.hasNext()) {
                            return;
                        }

                        player = (PlayerEntity) var2.next();
                    } while (!PlayerPositionTracker.isInView(player));
                } while (!player.botEntity);
                if (mc.gameSettings.getPointOfView() != PointOfView.FIRST_PERSON) {
                    break;
                }

                Minecraft var10001 = mc;
            } while (player == Minecraft.player);

            Vector4d position = PlayerPositionTracker.updatePlayerPositions(player, partialTicks);
            if (position != null) {
                this.positions.put(position, player);
            }
        }
    }

    private void renderPlayerElements(MatrixStack stack) {
        if (positions.isEmpty()) return;

        GL11.glEnable(GL11.GL_DEPTH_TEST);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        RenderSystem.disableTexture();
        RenderSystem.shadeModel(GL11.GL_SMOOTH);
        IMinecraft.BUFFER.begin(7, DefaultVertexFormats.POSITION_COLOR);

        boolean isConnectedToServer = Manager.FUNCTION_MANAGER.scoreboardHealth.state;
        PlayerEntity currentPlayer = getCurrentPlayer();

        List<Map.Entry<Vector4d, PlayerEntity>> sortedPositions = new ArrayList<>(positions.entrySet());
        sortedPositions.sort((a, b) -> {
            double distA = mc.player.getDistance(a.getValue());
            double distB = mc.player.getDistance(b.getValue());
            return Double.compare(distB, distA); // Farthest first
        });

        for (Map.Entry<Vector4d, PlayerEntity> entry : sortedPositions) {
            PlayerEntity player = entry.getValue();
            if (isConnectedToServer && player != null && !player.equals(currentPlayer)) {
                updatePlayerHealth(player);
            }
        }

        IMinecraft.TESSELLATOR.draw();
        RenderSystem.shadeModel(GL11.GL_FLAT);
        RenderSystem.enableTexture();
        RenderSystem.disableBlend();
        GL11.glDisable(GL11.GL_DEPTH_TEST);

        for (Map.Entry<Vector4d, PlayerEntity> entry : sortedPositions) {
            Vector4d position = entry.getKey();
            PlayerEntity player = entry.getValue();

            double x = position.x;
            double y = position.y;
            double endX = position.z;
            double endY = position.w;

            if (IMinecraft.mc.ingameGUI.getTabList().header == null) {
                continue;
            }
            renderTags(stack, (float) x, (float) y, (float) endX, (float) endY, player);
        }
    }

    private PlayerEntity getCurrentPlayer() {
        return Minecraft.getInstance().player;
    }

    private void updatePlayerHealth(PlayerEntity player) {
        String myPlayerName = String.valueOf(mc.player.getName());

        if (player.getName().getString().equals(myPlayerName)) {
            return;
        }

        for (Map.Entry<ScoreObjective, Score> entry : IMinecraft.mc.world.getScoreboard().getObjectivesForEntity(player.getName().getString()).entrySet()) {
            Score score = entry.getValue();
            int newHealth = score.getScorePoints();
            player.setHealth(Math.max(newHealth, 1));
        }
    }

    private void renderTags(MatrixStack matrixStack, float posX, float posY, float endPosX, float endPosY, PlayerEntity entity) {
        float maxOffsetY = 0.0F;
        ITextComponent text = entity.getDisplayName();
        TextComponent name = (TextComponent) text;

        String friendPrefix = "";
        ITextComponent friendText = ITextComponent.getTextComponentOrEmpty(friendPrefix);

        TextComponent friendPrefixComponent = (TextComponent) friendText;
        if (Manager.FRIEND_MANAGER.isFriend(entity.getName().getString()) && (Manager.FUNCTION_MANAGER.nameProtect.state && Manager.FUNCTION_MANAGER.nameProtect.friends.get())) {
            friendPrefixComponent.append(new StringTextComponent(TextFormatting.RED + (entity.getDisplayName().getString().contains("●") ? "    monoton.xyz" : "monoton.xyz")));
        } else {
            friendPrefixComponent.append(name);
        }
        name = friendPrefixComponent;

        int health = (int) entity.getHealth();
        float maxHealth = entity.getMaxHealth();
        float healthPercentage = (health / maxHealth) * 100;
        String colorCode;
        if (healthPercentage < 25) {
            colorCode = TextFormatting.RED.toString();
        } else if (healthPercentage < 50) {
            colorCode = TextFormatting.GOLD.toString();
        } else if (healthPercentage < 75) {
            colorCode = TextFormatting.YELLOW.toString();
        } else {
            colorCode = TextFormatting.GREEN.toString();
        }
        name.append(new StringTextComponent(colorCode + " " + health + "HP"));

        TextComponent finalName = name;
        float width = Fonts.intl[19].getWidth(finalName.getString()) + 6;
        float height = 15f;
        int colorsbox2 = Manager.FRIEND_MANAGER.isFriend(entity.getName().getString()) ? (new java.awt.Color(41, 228, 28, 55)).getRGB() : rgba(15, 15, 16, 125);

        MathUtil.scaleElements((posX + endPosX) / 2f, posY - height / 2, size.getValue().floatValue(), () -> {
            ClientVoicechat client = ClientManager.getClient();
            RenderUtilka.Render2D.drawRoundedRect((posX + endPosX) / 2.0F - width / 2.0F, posY - height - 11.0F + 2F + 0.75F, width, height - 2F, 0F, colorsbox2);
            if (client != null) {
                ClientPlayerStateManager manager = ClientManager.getPlayerStateManager();
                boolean isVoiceClient = !manager.isPlayerDisconnected(entity);
                if (elements.get(3) && isVoiceClient) {
                    boolean isSpeak = client.getTalkCache().isWhispering(entity) || client.getTalkCache().isTalking(entity);
                    int color_speak = manager.isPlayerDisabled(entity) ? (new Color(84, 0, 0, 255)).getRGB() :
                            isSpeak ? (new Color(0, 255, 0, 255)).getRGB() : (new Color(255, 0, 0, 255)).getRGB();
                    RenderUtilka.Render2D.drawRoundedRect((posX + endPosX) / 2.0F - width / 2.0F + width - 0.5f, posY - height - 11.0F + 2F + 0.75F, 2.25f, height - 2F, 0F, color_speak);
                    RenderUtilka.Render2D.drawRoundedRect((posX + endPosX) / 2.0F - width / 2.0F - 1.5f, posY - height - 11.0F + 2F + 0.75F, 2.25f, height - 2F, 0F, color_speak);
                }
            }
            Fonts.intl[19].drawText(matrixStack, finalName, (posX + endPosX) / 2f - width / 2f + (Fonts.intl[19].getWidth(finalName.getString())) - width + 8.5f, posY - height - 4.5f);
            if (entity.getDisplayName().getString().contains("§a●")) {
                RenderUtilka.Render2D.drawCircle2((posX + endPosX) / 2.0F - width / 2.0F - 1.5f + 5.25f, posY - height - 11.0F + 2F + 0.75F + 3.5f, 3F, new Color(84, 252, 84, 255).getRGB());
            } else if (entity.getDisplayName().getString().contains("§c●")) {
                RenderUtilka.Render2D.drawCircle2((posX + endPosX) / 2.0F - width / 2.0F - 1.5f + 5.25f, posY - height - 11.0F + 2F + 0.75F + 3.5f, 3F, new Color(252, 84, 84, 255).getRGB());
            } else if (entity.getDisplayName().getString().contains("§6●")) {
                RenderUtilka.Render2D.drawCircle2((posX + endPosX) / 2.0F - width / 2.0F - 1.5f + 5.25f, posY - height - 11.0F + 2F + 0.75F + 3.5f, 3F, new Color(252, 168, 0, 255).getRGB());
            } else if (entity.getDisplayName().getString().contains("●")) {
                RenderUtilka.Render2D.drawCircle2((posX + endPosX) / 2.0F - width / 2.0F - 1.5f + 5.25f, posY - height - 11.0F + 2F + 0.75F + 3.5f, 3F, new Color(252, 84, 84, 255).getRGB());
            }

            ItemStack offhandItem = entity.getHeldItemOffhand();
            boolean isSkull = offhandItem.getItem() instanceof SkullItem;
            boolean isEnchantedTotem = false;
            if (offhandItem.getItem() == Items.TOTEM_OF_UNDYING) {
                CompoundNBT tag = offhandItem.getTag();
                if (tag != null && tag.contains("Enchantments")) {
                    isEnchantedTotem = true;
                }
            }
            if (elements.get(2) && !offhandItem.isEmpty() && (isSkull || isEnchantedTotem)) {
                ITextComponent offhandName = offhandItem.getDisplayName();
                float offhandWidth = Fonts.intl[17].getWidth(offhandName.getString()) + 6;
                float itemPosX = (posX + endPosX) / 2.0F - offhandWidth / 2.0F + 12 - 16 - 5;
                float itemPosY = posY - height - 24F;

                RenderUtilka.Render2D.drawRoundedRect(itemPosX + 0.25f, itemPosY + 0.25f, 14.5f, 14.5f, 0.5f, new java.awt.Color(246, 50, 50, 76).getRGB());

                RenderSystem.pushMatrix();
                RenderSystem.scaled(1.0 / 1.08, 1.0 / 1.08, 1.0); // Scale down by 3
                drawItemStack(offhandItem, itemPosX * 1.08, itemPosY * 1.08, null, false);
                RenderSystem.popMatrix();

                RenderUtilka.Render2D.drawRoundedRect((posX + endPosX) / 2.0F - offhandWidth / 2.0F + 12 - 5, posY - height - 23.5F + 0.75F, offhandWidth, height - 3F, 0F, rgba(15, 15, 16, 125));
                Fonts.intl[17].drawText(matrixStack, offhandName, (posX + endPosX) / 2f - offhandWidth / 2f + (Fonts.intl[17].getWidth(offhandName.getString())) - offhandWidth + 8.5f + 12 - 5, posY - height - 19f);
            }
        });

        maxOffsetY += 20.5F;

        List<ItemStack> stacks = new ArrayList<>(Collections.singletonList(entity.getHeldItemMainhand()));
        entity.getArmorInventoryList().forEach(stacks::add);
        stacks.removeIf(w -> w.getItem() instanceof AirItem);

        ItemStack offhandItem = entity.getHeldItemOffhand();
        boolean isSkull = offhandItem.getItem() instanceof SkullItem;
        boolean isEnchantedTotem = false;
        if (offhandItem.getItem() == Items.TOTEM_OF_UNDYING) {
            CompoundNBT tag = offhandItem.getTag();
            if (tag != null && tag.contains("Enchantments")) {
                isEnchantedTotem = true;
            }
        }

        int totalSize = stacks.size() * 20;
        maxOffsetY += (elements.get(2) && !offhandItem.isEmpty() && (isSkull || isEnchantedTotem) ? 25.0f * size.getValue().floatValue() : 7.0f * size.getValue().floatValue()) + (size.getValue().floatValue() * 10) + 5;

        AtomicInteger iterable = new AtomicInteger();
        if (elements.get(0)) {
            float finalMaxOffsetY = maxOffsetY;
            MathUtil.scaleElements((posX + endPosX) / 2.0F, posY - maxOffsetY / 2.0F, size.getValue().floatValue(), () -> {
                this.renderArmorAndEnchantment(stacks, matrixStack, posX, endPosX, posY, finalMaxOffsetY, totalSize, iterable, entity);
            });
        }
    }

    public static void drawItemStack(ItemStack stack,
                                     double x,
                                     double y,
                                     String altText,
                                     boolean withoutOverlay) {
        RenderSystem.translated(x, y, 0.0);
        IMinecraft.mc.getItemRenderer().renderItemAndEffectIntoGUI(stack, 0, 0);
        if (!withoutOverlay) {
            IMinecraft.mc.getItemRenderer().renderItemOverlayIntoGUI(IMinecraft.mc.fontRenderer, stack, 0, 0, altText);
        }
        RenderSystem.translated(-x, -y, 0.0);
    }

    private void renderArmorAndEnchantment(List<ItemStack> stacks, MatrixStack matrixStack, float posX, float endPosX, float posY, float finalMaxOffsetY, int totalSize, AtomicInteger iterable, PlayerEntity entity) {
        List<ItemStack> sortedStacks = new ArrayList<>();
        ItemStack mainHand = entity.getHeldItemMainhand();
        ItemStack offHand = entity.getHeldItemOffhand();
        List<ItemStack> armorInventory = new ArrayList<>();
        entity.getArmorInventoryList().forEach(armorInventory::add);
        ItemStack head = armorInventory.size() > 3 ? armorInventory.get(3) : null;
        ItemStack chest = armorInventory.size() > 2 ? armorInventory.get(2) : null;
        ItemStack legs = armorInventory.size() > 1 ? armorInventory.get(1) : null;
        ItemStack feet = armorInventory.size() > 0 ? armorInventory.get(0) : null;

        if (mainHand != null && !mainHand.isEmpty()) sortedStacks.add(mainHand);

        if (offHand != null && !offHand.isEmpty()) {
            boolean isSkull = offHand.getItem() instanceof SkullItem;
            boolean isEnchantedTotem = offHand.getItem() == Items.TOTEM_OF_UNDYING && offHand.getTag() != null && offHand.getTag().contains("Enchantments");
            if (!elements.get(2) || (!isSkull && !isEnchantedTotem)) {
                sortedStacks.add(offHand);
            }
        }

        if (head != null && !head.isEmpty()) sortedStacks.add(head);
        if (chest != null && !chest.isEmpty()) sortedStacks.add(chest);
        if (legs != null && !legs.isEmpty()) sortedStacks.add(legs);
        if (feet != null && !feet.isEmpty()) sortedStacks.add(feet);

        float centerX = (posX + endPosX) / 2f;

        for (ItemStack stack : sortedStacks) {
            if (stack.isEmpty()) {
                continue;
            }

            float itemPosX = centerX - (sortedStacks.size() * 16f / 2f) + iterable.get() * 16f;
            float itemPosY = posY - finalMaxOffsetY;

            boolean isSkull = stack.getItem() instanceof SkullItem;
            boolean isEnchantedTotem = false;

            if (stack.getItem() == Items.TOTEM_OF_UNDYING) {
                CompoundNBT tag = stack.getTag();
                if (tag != null && tag.contains("Enchantments")) {
                    isEnchantedTotem = true;
                }
            }

            if (isSkull || isEnchantedTotem) {
                RenderUtilka.Render2D.drawRoundedRect(itemPosX, itemPosY, 16, 16, 0.5f,
                        new java.awt.Color(246, 50, 50, 76).getRGB());
            }

            drawItemStack(stack, itemPosX, itemPosY, null, false);
            iterable.getAndIncrement();

            if (elements.get(1)) {
                ArrayList<String> enchantments = getEnchantment(stack);
                float enchantmentPosX = itemPosX + 8;
                float enchantmentPosY = itemPosY - 5;
                int i = 0;

                for (String enchantment : enchantments) {
                    Fonts.intl[12].drawCenteredString(matrixStack, enchantment,
                            enchantmentPosX,
                            enchantmentPosY - (i * 7),
                            0xFFFFFFFF);
                    i++;
                }
            }
        }
    }

    private ArrayList<String> getEnchantment(ItemStack stack) {
        ArrayList<String> list = new ArrayList<>();
        Item item = stack.getItem();
        if (item instanceof AxeItem) {
            handleAxeEnchantments(list, stack);
        } else if (item instanceof ArmorItem) {
            handleArmorEnchantments(list, stack);
        } else if (item instanceof BowItem) {
            handleBowEnchantments(list, stack);
        } else if (item instanceof SwordItem) {
            handleSwordEnchantments(list, stack);
        } else if (item instanceof ToolItem) {
            handleToolEnchantments(list, stack);
        }
        return list;
    }

    private void handleAxeEnchantments(ArrayList<String> list, ItemStack stack) {
        int sharpness = EnchantmentHelper.getEnchantmentLevel(Enchantments.SHARPNESS, stack);
        int efficiency = EnchantmentHelper.getEnchantmentLevel(Enchantments.EFFICIENCY, stack);
        int unbreaking = EnchantmentHelper.getEnchantmentLevel(Enchantments.UNBREAKING, stack);
        int fireAspect = EnchantmentHelper.getEnchantmentLevel(Enchantments.FIRE_ASPECT, stack);

        if (sharpness > 0) {
            if (sharpness > 5) {
                list.add(TextFormatting.RED + "S" + sharpness);
            } else {
                list.add("S" + sharpness);
            }
        }
        if (efficiency > 0) {
            list.add("E" + efficiency);
        }
        if (unbreaking > 0) {
            list.add("U" + unbreaking);
        }
        if (fireAspect > 0) {
            list.add("F" + fireAspect);
        }
    }

    private void handleArmorEnchantments(ArrayList<String> list, ItemStack stack) {
        int protection = EnchantmentHelper.getEnchantmentLevel(Enchantments.PROTECTION, stack);
        int thorns = EnchantmentHelper.getEnchantmentLevel(Enchantments.THORNS, stack);
        int unbreaking = EnchantmentHelper.getEnchantmentLevel(Enchantments.UNBREAKING, stack);
        int mending = EnchantmentHelper.getEnchantmentLevel(Enchantments.MENDING, stack);
        int depth = EnchantmentHelper.getEnchantmentLevel(Enchantments.DEPTH_STRIDER, stack);
        int vanishingCurse = EnchantmentHelper.getEnchantmentLevel(Enchantments.VANISHING_CURSE, stack);
        int bindingCurse = EnchantmentHelper.getEnchantmentLevel(Enchantments.BINDING_CURSE, stack);

        if (vanishingCurse > 0) {
            list.add("V");
        }
        if (bindingCurse > 0) {
            list.add("B" + bindingCurse);
        }
        if (depth > 0) {
            list.add("D" + depth);
        }
        if (protection > 0) {
            if (protection > 4) {
                list.add(TextFormatting.RED + "P" + protection);
            } else {
                list.add("P" + protection);
            }
        }
        if (thorns > 0) {
            list.add("T" + thorns);
        }
        if (mending > 0) {
            list.add("M" + mending);
        }
        if (unbreaking > 0) {
            list.add("U" + unbreaking);
        }
    }

    private void handleBowEnchantments(ArrayList<String> list, ItemStack stack) {
        int vanishingCurse = EnchantmentHelper.getEnchantmentLevel(Enchantments.VANISHING_CURSE, stack);
        int bindingCurse = EnchantmentHelper.getEnchantmentLevel(Enchantments.BINDING_CURSE, stack);
        int infinity = EnchantmentHelper.getEnchantmentLevel(Enchantments.INFINITY, stack);
        int power = EnchantmentHelper.getEnchantmentLevel(Enchantments.POWER, stack);
        int punch = EnchantmentHelper.getEnchantmentLevel(Enchantments.PUNCH, stack);
        int mending = EnchantmentHelper.getEnchantmentLevel(Enchantments.MENDING, stack);
        int flame = EnchantmentHelper.getEnchantmentLevel(Enchantments.FLAME, stack);
        int unbreaking = EnchantmentHelper.getEnchantmentLevel(Enchantments.UNBREAKING, stack);

        if (vanishingCurse > 0) {
            list.add("V" + vanishingCurse);
        }
        if (bindingCurse > 0) {
            list.add("B" + bindingCurse);
        }
        if (infinity > 0) {
            list.add("I" + infinity);
        }
        if (power > 0) {
            list.add("P" + power);
        }
        if (punch > 0) {
            list.add("P" + punch);
        }
        if (mending > 0) {
            list.add("M" + mending);
        }
        if (flame > 0) {
            list.add("F" + flame);
        }
        if (unbreaking > 0) {
            list.add("U" + unbreaking);
        }
    }

    private void handleSwordEnchantments(ArrayList<String> list, ItemStack stack) {
        int vanishingCurse = EnchantmentHelper.getEnchantmentLevel(Enchantments.VANISHING_CURSE, stack);
        int looting = EnchantmentHelper.getEnchantmentLevel(Enchantments.LOOTING, stack);
        int bindingCurse = EnchantmentHelper.getEnchantmentLevel(Enchantments.BINDING_CURSE, stack);
        int sweeping = EnchantmentHelper.getEnchantmentLevel(Enchantments.SWEEPING, stack);
        int sharpness = EnchantmentHelper.getEnchantmentLevel(Enchantments.SHARPNESS, stack);
        int knockback = EnchantmentHelper.getEnchantmentLevel(Enchantments.KNOCKBACK, stack);
        int fireAspect = EnchantmentHelper.getEnchantmentLevel(Enchantments.FIRE_ASPECT, stack);
        int unbreaking = EnchantmentHelper.getEnchantmentLevel(Enchantments.UNBREAKING, stack);
        int mending = EnchantmentHelper.getEnchantmentLevel(Enchantments.MENDING, stack);

        if (vanishingCurse > 0) {
            list.add("V" + vanishingCurse);
        }
        if (looting > 0) {
            list.add("L" + looting);
        }
        if (bindingCurse > 0) {
            list.add("B" + bindingCurse);
        }
        if (sweeping > 0) {
            list.add("S" + sweeping);
        }
        if (sharpness > 0) {
            if (sharpness > 5) {
                list.add(TextFormatting.RED + "S" + sharpness);
            } else {
                list.add("S" + sharpness);
            }
        }
        if (knockback > 0) {
            list.add("K" + knockback);
        }
        if (fireAspect > 0) {
            list.add("F" + fireAspect);
        }
        if (unbreaking > 0) {
            list.add("U" + unbreaking);
        }
        if (mending > 0) {
            list.add("M" + mending);
        }
    }

    private void handleToolEnchantments(ArrayList<String> list, ItemStack stack) {
        int unbreaking = EnchantmentHelper.getEnchantmentLevel(Enchantments.UNBREAKING, stack);
        int mending = EnchantmentHelper.getEnchantmentLevel(Enchantments.MENDING, stack);
        int vanishingCurse = EnchantmentHelper.getEnchantmentLevel(Enchantments.VANISHING_CURSE, stack);
        int bindingCurse = EnchantmentHelper.getEnchantmentLevel(Enchantments.BINDING_CURSE, stack);
        int efficiency = EnchantmentHelper.getEnchantmentLevel(Enchantments.EFFICIENCY, stack);
        int silkTouch = EnchantmentHelper.getEnchantmentLevel(Enchantments.SILK_TOUCH, stack);
        int fortune = EnchantmentHelper.getEnchantmentLevel(Enchantments.FORTUNE, stack);

        if (unbreaking > 0) {
            list.add("U" + unbreaking);
        }
        if (mending > 0) {
            list.add("M" + mending);
        }
        if (vanishingCurse > 0) {
            list.add("V" + vanishingCurse);
        }
        if (bindingCurse > 0) {
            list.add("B" + bindingCurse);
        }
        if (efficiency > 0) {
            list.add("E" + efficiency);
        }
        if (silkTouch > 0) {
            list.add("S" + silkTouch);
        }
        if (fortune > 0) {
            list.add("F" + fortune);
        }
    }
}