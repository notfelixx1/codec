package monoton.module.impl.render;

import monoton.control.events.client.Event;
import monoton.control.events.player.EventKill;
import monoton.module.TypeList;
import monoton.module.api.Annotation;
import monoton.module.api.Module;
import monoton.module.settings.imp.ModeSetting;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.LightningBoltEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particles.ParticleTypes;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.block.Blocks;

@Annotation(name = "KillEffect", type = TypeList.Render, desc = "Эффекты при убийстве игроков")
public class KillEffect extends Module {

    private final ModeSetting effect = new ModeSetting("Эффект", "Молния", "Молния", "Кровь", "Взрыв", "Сердце", "Дым");

    public KillEffect() {
        addSettings(effect);
    }

    @Override
    public boolean onEvent(Event event) {
        if (event instanceof EventKill killEvent) {
            LivingEntity target = killEvent.getTarget();

            if (target instanceof PlayerEntity) {
                killEffect(target);
            }
        }
        return false;
    }

    private void killEffect(LivingEntity entity) {
        if (entity == null || mc.player == null || mc.world == null) return;

        Vector3d pos = entity.getPositionVec();
        BlockPos blockPos = new BlockPos(pos);

        switch (effect.get()) {
            case "Молния":
                if (mc.worldRenderer != null) {
                    LightningBoltEntity lightning = new LightningBoltEntity(net.minecraft.entity.EntityType.LIGHTNING_BOLT, mc.world);
                    lightning.setPosition(pos.x, pos.y, pos.z);
                    lightning.setEffectOnly(true);
                    mc.world.addEntity(mc.world.getCountLoadedEntities() + 10000, lightning);
                    mc.world.playSound(mc.player, blockPos, SoundEvents.ENTITY_LIGHTNING_BOLT_THUNDER, SoundCategory.WEATHER, 5f, 1f);
                }
                break;

            case "Кровь":
                if (mc.worldRenderer != null) {
                    mc.worldRenderer.playEvent(mc.player, 2001, blockPos, net.minecraft.block.Block.getStateId(Blocks.REDSTONE_BLOCK.getDefaultState()));
                }
                break;

            case "Взрыв":
                for (int i = 0; i < 30; i++) {
                    mc.world.addParticle(ParticleTypes.EXPLOSION,
                            pos.x + (Math.random() - 0.5) * 3,
                            pos.y + Math.random() * 2,
                            pos.z + (Math.random() - 0.5) * 3,
                            (Math.random() - 0.5) * 0.2, Math.random() * 0.2, (Math.random() - 0.5) * 0.2);
                }
                mc.world.playSound(mc.player, blockPos, SoundEvents.ENTITY_GENERIC_EXPLODE, SoundCategory.BLOCKS, 1f, 1f);
                break;

            case "Сердце":
                for (int i = 0; i < 20; i++) {
                    mc.world.addParticle(ParticleTypes.HEART,
                            pos.x + (Math.random() - 0.5) * 2,
                            pos.y + Math.random() * 2,
                            pos.z + (Math.random() - 0.5) * 2,
                            0, 0.1, 0);
                }
                mc.world.playSound(mc.player, blockPos, SoundEvents.ENTITY_PLAYER_LEVELUP, SoundCategory.PLAYERS, 0.5f, 1.5f);
                break;

            case "Дым":
                for (int i = 0; i < 50; i++) {
                    mc.world.addParticle(ParticleTypes.LARGE_SMOKE,
                            pos.x + (Math.random() - 0.5) * 3,
                            pos.y + Math.random() * 2,
                            pos.z + (Math.random() - 0.5) * 3,
                            (Math.random() - 0.5) * 0.05, Math.random() * 0.1, (Math.random() - 0.5) * 0.05);
                }
                mc.world.playSound(mc.player, blockPos, SoundEvents.BLOCK_FIRE_EXTINGUISH, SoundCategory.BLOCKS, 1f, 1f);
                break;
        }
    }
}