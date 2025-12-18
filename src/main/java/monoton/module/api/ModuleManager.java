package monoton.module.api;

import monoton.module.impl.combat.*;
import monoton.module.impl.misc.*;
import monoton.module.impl.movement.*;
import monoton.module.impl.player.*;
import monoton.module.impl.render.*;
import monoton.module.impl.render.AspectRatio;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class ModuleManager {

    private List<Module> modules = new CopyOnWriteArrayList<>();
    public Arrows arrowsFunction;
    public Gamma fullBrightFunction;
    public Sprint sprint;
    public Strafe strafe;
    public NoPlaceSpher noPlaceSpher;
    public AntiTarget antiTarget;
    public SyncTps syncTps;
    public Timer timer;
    public StreamerMode streamerMode;
    public AutoPotion autoPotionFunction;
    public AutoRespawn autoRespawnFunction;
    public Velocity velocityFunction;
    public ClickPearl middleClickPearlFunction;
    public GuiMove guiMoveFunction;
    public AntiBot antiBot;
    public NoPush noPushFunction;
    public HitBox hitBoxFunction;
    public NoSlow noSlow;
    public SeeInvisibles seeInvisibles;
    public Aura auraFunction;
    public ElyrtaPredict elyrtaPredict;
    public CustomSwing swingAnimationFunction;
    public NoRender noRenderFunction;
    public ChinaHat chinaHat;
    public ItemsCooldown itemsCooldown;
    public Optimization optimization;
    public ServerJoiner serverJoiner;
    public AutoSwap autoSwapFunction;
    public AspectRatio aspectRatio;
    public ItemScroller itemScroller;
    public NameTags nameTags;
    public ElytraBooster elytraBooster;
    public TargetPearl targetPearl;
    public NoInteract noInteractFunction;
    public ScoreboardHealth scoreboardHealth;
    public Ambience ambience;
    public ArmorDurability armorDurability;
    public ElytraHelper elytraHelper;
    public ServerHelper serverHelper;
    public ClientSounds clientSounds;
    public Crosshair crosshair;
    public NameProtect nameProtect;
    public ViewMode viewMode;
    public Hud hud;
    public AutoExplosion autoExplosionFunction;
    public FreeCam freeCam;
    public WaterSpeed waterSpeed;
    public ProjectileHelper projectileHelper;
    public ClickGui clickGui;
    public NoCommands noCommands;
    public ItemPhysics itemPhysics;
    public SaturationViewer saturationViewer;
    public CatFly catFly;
    public BetterChat betterChat;
    public TargetEsp targetEsp;
    public Spider spider;
    public IRC irc;
    public FireworkEsp fireworkEsp;
    public ElytraMotion elytraMotion;
    public NoWeb noWeb;
    public Speed speed;
    public ShulkerPreview shulkerViewer;
    public CustomModel customModel;
    public Jesus jesus;
    public Predictions predictions;
    public Particles particles;
    public WebTrap webTrap;
    public SkeletonEsp skeletonEsp;
    public AutoMessage autoMessage;
    public BackTrack backTrack;
    public PacketCriticals packetCriticals;
    public SantaHat santaHat;

    public ModuleManager() {
    }

    public void initialize() {
        this.modules.addAll(Arrays.asList(
                this.clickGui = new ClickGui(),
                this.shulkerViewer = new ShulkerPreview(),
                this.targetPearl = new TargetPearl(),
                this.customModel = new CustomModel(),
                this.packetCriticals = new PacketCriticals(),
                this.chinaHat = new ChinaHat(),
                this.fireworkEsp = new FireworkEsp(),
                this.elytraBooster = new ElytraBooster(),
                this.crosshair = new Crosshair(),
                this.noWeb = new NoWeb(),
                this.speed = new Speed(),
                this.spider = new Spider(),
                this.arrowsFunction = new Arrows(),
                this.skeletonEsp = new SkeletonEsp(),
                this.serverJoiner = new ServerJoiner(),
                this.streamerMode = new StreamerMode(),
                this.antiBot = new AntiBot(),
                this.particles = new Particles(),
                this.serverHelper = new ServerHelper(),
                this.santaHat = new SantaHat(),
                this.betterChat = new BetterChat(),
                this.autoMessage = new AutoMessage(),
                this.backTrack = new BackTrack(),
                this.jesus = new Jesus(),
                this.elytraMotion = new ElytraMotion(),
                this.fullBrightFunction = new Gamma(),
                this.irc = new IRC(),
                this.noRenderFunction = new NoRender(),
                this.sprint = new Sprint(),
                this.predictions = new Predictions(),
                this.seeInvisibles = new SeeInvisibles(),
                this.strafe = new Strafe(),
                this.timer = new Timer(),
                this.velocityFunction = new Velocity(),
                this.middleClickPearlFunction = new ClickPearl(),
                this.projectileHelper = new ProjectileHelper(),
                this.guiMoveFunction = new GuiMove(),
                this.noPlaceSpher = new NoPlaceSpher(),
                this.autoRespawnFunction = new AutoRespawn(),
                this.autoSwapFunction = new AutoSwap(),
                this.syncTps = new SyncTps(),
                this.noPushFunction = new NoPush(),
                this.hitBoxFunction = new HitBox(),
                this.noSlow = new NoSlow(),
                this.waterSpeed = new WaterSpeed(),
                this.noCommands = new NoCommands(),
                this.antiTarget = new AntiTarget(),
                this.autoPotionFunction = new AutoPotion(),
                this.hud = new Hud(),
                this.swingAnimationFunction = new CustomSwing(),
                this.itemsCooldown = new ItemsCooldown(),
                this.optimization = new Optimization(),
                this.saturationViewer = new SaturationViewer(),
                this.itemScroller = new ItemScroller(),
                this.aspectRatio = new AspectRatio(),
                this.scoreboardHealth = new ScoreboardHealth(),
                this.noInteractFunction = new NoInteract(),
                this.ambience = new Ambience(),
                this.armorDurability = new ArmorDurability(),
                this.clientSounds = new ClientSounds(),
                this.nameProtect = new NameProtect(),
                this.itemPhysics = new ItemPhysics(),
                this.viewMode = new ViewMode(),
                this.elyrtaPredict = new ElyrtaPredict(),
                this.auraFunction = new Aura(),
                this.catFly = new CatFly(),
                this.targetEsp = new TargetEsp(),
                this.elytraHelper = new ElytraHelper(),
                this.webTrap = new WebTrap(),
                new ChestStealer(),
                new NoFriendDamage(),
                new Animation(),
                new ItemEsp(),
                new ElytraJump(),
                new AutoTpaccept(),
                new ClickFriend(),
                new Tracers(),
                new JumpCircle(),
                this.autoExplosionFunction = new AutoExplosion(),
                new Trails(),
                new AutoContract(),
                new RegionExploit(),
                new NoPlayerInteract(),
                new DeathCoords(),
                new StaffKill(),
                this.freeCam = new FreeCam(),
                new BlockEsp(),
                this.nameTags = new NameTags(),
                new TNTTimer(),
                new AutoDupe(),
                new AutoLeave(),
                new InvSync(),
                new NoFall(),
                new Chams(),
                new Flight(),
                new KillEffect(),
                new NoDelay(),
                new AutoArmor(),
                new AutoFish(),
                new LockSlot(),
                new UseTracker(),
                new AutoEat(),
                new ItemRelease(),
                new AutoPilot(),
                new FastBreak(),
                new CrashCrack(),
                new ItemHelper(),
                new TapeMouse(),
                new AutoDuel(),
                new LeaveTracker(),
                new AutoTotem(),
                new HighJump(),
                new BlockOverlay(),
                new NoClip(),
                new DragonFly(),
                new CrystallOptimizer(),
                new ItemSwapFix(),
                new TriggerBot(),
                new AutoTool()
        ));
    }

    public List<Module> getFunctions() {
        return modules;
    }

    public Module get(String name) {
        for (Module module : modules) {
            if (module != null && module.name.equalsIgnoreCase(name)) {
                return module;
            }
        }
        return null;
    }
}
