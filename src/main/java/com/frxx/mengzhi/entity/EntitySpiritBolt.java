package com.frxx.mengzhi.entity;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.MoverType;
import net.minecraft.init.SoundEvents;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.List;
import java.util.UUID;

/** 灵气弹：无重力、可追踪目标（类似潜影贝弹射物），命中造成 法术伤害x0.5 */
public class EntitySpiritBolt extends Entity {

    private static final double SPEED = 0.9D;
    private static final int MAX_AGE = 200;

    private UUID ownerId;
    private EntityLivingBase owner;
    private EntityLivingBase target;
    private double damage;

    public EntitySpiritBolt(World world) {
        super(world);
        setSize(0.4F, 0.4F);
    }

    public EntitySpiritBolt(World world, EntityLivingBase owner, double damage) {
        this(world);
        this.owner = owner;
        this.ownerId = owner.getUniqueID();
        this.damage = damage;
        Vec3d eye = owner.getPositionEyes(1.0F);
        setPosition(eye.x, eye.y, eye.z);
        Vec3d look = owner.getLook(1.0F);
        motionX = look.x * SPEED;
        motionY = look.y * SPEED;
        motionZ = look.z * SPEED;
        this.target = acquireTarget(owner, eye, look);
    }

    @Override
    protected void entityInit() {
    }

    @Override
    protected void readEntityFromNBT(NBTTagCompound compound) {
        if (compound.hasUniqueId("Owner")) {
            ownerId = compound.getUniqueId("Owner");
        }
        damage = compound.getDouble("Damage");
    }

    @Override
    protected void writeEntityToNBT(NBTTagCompound compound) {
        if (ownerId != null) {
            compound.setUniqueId("Owner", ownerId);
        }
        compound.setDouble("Damage", damage);
    }

    @Override
    public boolean canBeCollidedWith() {
        return false;
    }

    @Override
    public void onUpdate() {
        prevPosX = posX;
        prevPosY = posY;
        prevPosZ = posZ;
        super.onUpdate();

        if (world.isRemote) {
            move(MoverType.SELF, motionX, motionY, motionZ);
            if (ticksExisted % 2 == 0) {
                world.spawnParticle(EnumParticleTypes.REDSTONE, posX, posY, posZ, 1, 0, 0, 0, 210, 80, 255);
            }
            return;
        }

        if (ticksExisted > MAX_AGE) {
            fizzle();
            return;
        }

        if (owner == null && ownerId != null) {
            for (Object o : world.loadedEntityList) {
                Entity e = (Entity) o;
                if (e instanceof EntityLivingBase && e.getUniqueID().equals(ownerId)) {
                    owner = (EntityLivingBase) e;
                    break;
                }
            }
        }

        if (target == null || !target.isEntityAlive()) {
            target = findTarget();
        }

        if (target != null) {
            Vec3d to = new Vec3d(target.posX - posX, target.posY + target.height * 0.5 - posY, target.posZ - posZ);
            double dist = to.lengthVector();
            if (dist < 0.8) {
                hit(target);
                return;
            }
            Vec3d dir = to.normalize();
            motionX += (dir.x * SPEED - motionX) * 0.12;
            motionY += (dir.y * SPEED - motionY) * 0.12;
            motionZ += (dir.z * SPEED - motionZ) * 0.12;
        } else {
            double spd = Math.sqrt(motionX * motionX + motionY * motionY + motionZ * motionZ);
            if (spd > 0.0001) {
                double f = SPEED / spd;
                motionX *= f;
                motionY *= f;
                motionZ *= f;
            }
        }

        move(MoverType.SELF, motionX, motionY, motionZ);

        if (world.collidesWithAnyBlock(getEntityBoundingBox())) {
            hit(null);
            return;
        }

        EntityLivingBase other = findOtherHit();
        if (other != null) {
            hit(other);
            return;
        }
    }

    private void hit(EntityLivingBase entity) {
        explodeEffects();
        if (entity != null) {
            if (damage > 0) {
                entity.attackEntityFrom(DamageSource.causeIndirectMagicDamage(this, owner), (float) damage);
            }
            double dx = entity.posX - posX;
            double dy = entity.posY - posY;
            double dz = entity.posZ - posZ;
            double len = Math.sqrt(dx * dx + dy * dy + dz * dz);
            if (len > 0.0001) {
                entity.motionX += dx / len * 0.35;
                entity.motionZ += dz / len * 0.35;
                entity.velocityChanged = true;
            }
        }
        setDead();
    }

    private void fizzle() {
        explodeEffects();
        setDead();
    }

    private void explodeEffects() {
        world.playSound(null, posX, posY, posZ, SoundEvents.ENTITY_GENERIC_EXPLODE, SoundCategory.BLOCKS, 1.2F, 1.6F);
        world.spawnParticle(EnumParticleTypes.EXPLOSION_LARGE, posX, posY, posZ, 1, 0, 0, 0, 0);
        for (int i = 0; i < 10; i++) {
            world.spawnParticle(EnumParticleTypes.REDSTONE, posX, posY, posZ, 1, 0, 0, 0, 255, 120, 255);
        }
    }

    /** 发射瞬间按视线方向锁定目标（类似潜影贝） */
    private EntityLivingBase acquireTarget(EntityLivingBase shooter, Vec3d start, Vec3d look) {
        double range = 24.0;
        EntityLivingBase best = null;
        double bestT = Double.MAX_VALUE;
        AxisAlignedBB search = shooter.getEntityBoundingBox()
            .expand(look.x * range, look.y * range, look.z * range).expand(1.5, 1.5, 1.5);
        List<EntityLivingBase> list = world.getEntitiesWithinAABB(EntityLivingBase.class, search);
        for (EntityLivingBase e : list) {
            if (e == shooter || !e.isEntityAlive()) continue;
            double tx = e.posX - start.x;
            double ty = e.posY + e.height * 0.5 - start.y;
            double tz = e.posZ - start.z;
            double t = tx * look.x + ty * look.y + tz * look.z;
            if (t <= 0 || t > range) continue;
            double px = start.x + look.x * t;
            double py = start.y + look.y * t;
            double pz = start.z + look.z * t;
            double dx = e.posX - px;
            double dy = e.posY + e.height * 0.5 - py;
            double dz = e.posZ - pz;
            if (dx * dx + dy * dy + dz * dz > 1.44) continue;
            if (t < bestT) {
                bestT = t;
                best = e;
            }
        }
        return best;
    }

    /** 目标失效后的就近重新追踪 */
    private EntityLivingBase findTarget() {
        EntityLivingBase best = null;
        double bestD = 24.0 * 24.0;
        List<EntityLivingBase> list = world.getEntitiesWithinAABB(EntityLivingBase.class,
            getEntityBoundingBox().expand(16, 16, 16));
        for (EntityLivingBase e : list) {
            if (e == owner || !e.isEntityAlive()) continue;
            double d = getDistanceSq(e);
            if (d < bestD) {
                bestD = d;
                best = e;
            }
        }
        return best;
    }

    /** 撞上沿途的生物 */
    private EntityLivingBase findOtherHit() {
        List<EntityLivingBase> list = world.getEntitiesWithinAABB(EntityLivingBase.class,
            getEntityBoundingBox().expand(0.4, 0.4, 0.4));
        for (EntityLivingBase e : list) {
            if (e == owner) continue;
            return e;
        }
        return null;
    }
}
