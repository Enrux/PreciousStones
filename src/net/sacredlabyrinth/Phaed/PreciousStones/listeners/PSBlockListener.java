package net.sacredlabyrinth.Phaed.PreciousStones.listeners;

import net.sacredlabyrinth.Phaed.PreciousStones.*;
import net.sacredlabyrinth.Phaed.PreciousStones.entries.BlockTypeEntry;
import net.sacredlabyrinth.Phaed.PreciousStones.entries.CuboidEntry;
import net.sacredlabyrinth.Phaed.PreciousStones.vectors.Field;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.*;

import java.util.List;
import java.util.Set;

/**
 * PreciousStones block listener
 *
 * @author Phaed
 */
public class PSBlockListener implements Listener
{
    private PreciousStones plugin;

    /**
     *
     */
    public PSBlockListener()
    {
        plugin = PreciousStones.getInstance();
    }

    /**
     * @param event
     */
    @EventHandler(priority = EventPriority.NORMAL)
    public void onBlockFade(BlockFadeEvent event)
    {
        //If the block is going to disappear because it's a field.(leaves, ice, etc)
        //Cancel the event

        if (plugin.getForceFieldManager().isField(event.getBlock()))
        {
            event.setCancelled(true);
        }
    }

    /**
     * @param event
     */

    @EventHandler(priority = EventPriority.NORMAL)
    public void onBlockFromTo(BlockFromToEvent event)
    {
        DebugTimer dt = new DebugTimer("onBlockFromTo");

        Block source = event.getBlock();
        Block destination = event.getToBlock();

        if (plugin.getSettingsManager().isBlacklistedWorld(source.getWorld()))
        {
            return;
        }

        if (Helper.isSameBlock(source.getLocation(), destination.getLocation()))
        {
            return;
        }

        // if the block itself is a field cancel its flow
        // this allows for water and lava pstones

        Field blockField = plugin.getForceFieldManager().getField(source);

        if (blockField != null)
        {
            event.setCancelled(true);
        }

        // if the destination area is not protected, don't bother

        Field destField = plugin.getForceFieldManager().getEnabledSourceField(destination.getLocation(), FieldFlag.PREVENT_FLOW);

        if (destField == null)
        {
            return;
        }

        // if the source is outside protection, or if its protected by a different owner, then block the water

        Field sourceField = plugin.getForceFieldManager().getEnabledSourceField(source.getLocation(), FieldFlag.PREVENT_FLOW);

        if (sourceField == null || !sourceField.getOwner().equalsIgnoreCase(destField.getOwner()))
        {
            event.setCancelled(true);
        }

        if (plugin.getSettingsManager().isDebug())
        {
            dt.logProcessTime();
        }
    }

    /**
     * @param event
     */

    @EventHandler(priority = EventPriority.HIGH)
    public void onBlockIgnite(BlockIgniteEvent event)
    {
        if (event.isCancelled())
        {
            return;
        }

        Block block = event.getBlock();
        Player player = event.getPlayer();

        if (block == null)
        {
            return;
        }

        DebugTimer dt = new DebugTimer("onBlockIgnite");

        if (player != null)
        {
            plugin.getSnitchManager().recordSnitchIgnite(player, block);
        }

        Field field = plugin.getForceFieldManager().getEnabledSourceField(block.getLocation(), FieldFlag.PREVENT_FIRE);

        if (field != null)
        {
            if (player == null || !plugin.getForceFieldManager().isApplyToAllowed(field, player.getName()) || field.hasFlag(FieldFlag.APPLY_TO_ALL))
            {
                event.setCancelled(true);

                if (player != null)
                {
                    plugin.getCommunicationManager().warnFire(player, block, field);
                }
            }
        }

        if (plugin.getSettingsManager().isDebug())
        {
            dt.logProcessTime();
        }
    }

    /**
     * @param event
     */

    @EventHandler(priority = EventPriority.HIGH)
    public void onBlockRedstoneChange(BlockRedstoneEvent event)
    {
        List<Field> fields = plugin.getForceFieldManager().getSourceFields(event.getBlock().getLocation(), FieldFlag.ENABLE_WITH_REDSTONE);

        for (final Field field : fields)
        {
            // looking only for actionable fields

            if (!field.getSettings().hasDefaultFlag(FieldFlag.LAUNCH) &&
                    !field.getSettings().hasDefaultFlag(FieldFlag.CANNON) &&
                    !field.getSettings().hasDefaultFlag(FieldFlag.POTIONS) &&
                    !field.getSettings().hasDefaultFlag(FieldFlag.CONFISCATE_ITEMS))
            {
                continue;
            }

            // only act on fields that are being touched by this wire

            if (!plugin.getForceFieldManager().powersField(field, event.getBlock()))
            {
                continue;
            }

            Set<Player> inhabitants = plugin.getForceFieldManager().getFieldInhabitants(field);

            for (final Player player : inhabitants)
            {
                if (player != null)
                {
                    if (event.getNewCurrent() > event.getOldCurrent())
                    {
                        if (field.isDisabled())
                        {
                            field.setDisabled(false);
                        }

                        PreciousStones.debug("redstone enabled");
                    }
                    else if (event.getNewCurrent() == 0)
                    {
                        if (!field.isDisabled())
                        {
                            field.setDisabled(true);
                        }

                        PreciousStones.debug("redstone disabled");
                    }

                    plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, new Runnable()
                    {
                        public void run()
                        {
                            boolean allowed = plugin.getForceFieldManager().isApplyToAllowed(field, player.getName());

                            if (field.isDisabled())
                            {
                                if (allowed || field.hasFlag(FieldFlag.APPLY_TO_ALL))
                                {
                                    if (field.hasFlag(FieldFlag.POTIONS))
                                    {
                                        plugin.getPotionManager().removePotions(player, field);
                                    }
                                }

                                if (!allowed)
                                {
                                    if (field.hasFlag(FieldFlag.CONFISCATE_ITEMS))
                                    {
                                        plugin.getConfiscationManager().returnItems(player);
                                    }
                                }
                            }
                            else
                            {
                                if (allowed || field.hasFlag(FieldFlag.APPLY_TO_ALL))
                                {
                                    if (field.hasFlag(FieldFlag.LAUNCH))
                                    {
                                        plugin.getVelocityManager().launchPlayer(player, field);
                                    }

                                    if (field.hasFlag(FieldFlag.CANNON))
                                    {
                                        plugin.getVelocityManager().shootPlayer(player, field);
                                    }

                                    if (field.hasFlag(FieldFlag.POTIONS))
                                    {
                                        plugin.getPotionManager().applyPotions(player, field);
                                    }
                                }

                                if (!allowed)
                                {
                                    if (field.hasFlag(FieldFlag.CONFISCATE_ITEMS))
                                    {
                                        plugin.getConfiscationManager().confiscateItems(field, player);
                                    }
                                }
                            }
                        }
                    }, 0);
                }
            }
        }
    }

    /**
     * @param event
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onBlockBreak(BlockBreakEvent event)
    {
        if (event.isCancelled())
        {
            return;
        }

        Block block = event.getBlock();
        Player player = event.getPlayer();

        if (block == null || player == null)
        {
            return;
        }

        // do not allow break of non-field blocks during cuboid definition

        if (plugin.getCuboidManager().hasOpenCuboid(player) && !plugin.getSettingsManager().isFieldType(block))
        {
            event.setCancelled(true);
            return;
        }

        if (plugin.getSettingsManager().isBlacklistedWorld(block.getWorld()))
        {
            return;
        }

        if (plugin.getSettingsManager().isBypassBlock(block))
        {
            return;
        }

        DebugTimer dt = new DebugTimer("onBlockBreak");

        plugin.getSnitchManager().recordSnitchBlockBreak(player, block);


        // -------------------------------------------------------------------------------- prevent destroy everywhere

        if (plugin.getSettingsManager().isPreventDestroyEverywhere() && !plugin.getPermissionsManager().has(player, "preciousstones.bypass.destroy"))
        {
            boolean isAllowBlock = false;

            Field field = plugin.getForceFieldManager().getField(block);

            if (field != null)
            {
                if (field.hasFlag(FieldFlag.ALLOW_DESTROY) || field.hasFlag(FieldFlag.ALLOW_PLACE))
                {
                    isAllowBlock = true;
                }
            }

            if (!isAllowBlock)
            {
                field = plugin.getForceFieldManager().getEnabledSourceField(block.getLocation(), FieldFlag.ALLOW_DESTROY);

                if (field != null)
                {
                    boolean allowed = plugin.getForceFieldManager().isApplyToAllowed(field, player.getName());

                    if (!allowed && !field.hasFlag(FieldFlag.APPLY_TO_ALL))
                    {
                        event.setCancelled(true);
                        return;
                    }
                }
                else
                {
                    event.setCancelled(true);
                    return;
                }
            }
        }

        // -------------------------------------------------------------------------------- breaking a field

        if (plugin.getForceFieldManager().isField(block))
        {
            Field field = plugin.getForceFieldManager().getField(block);

            breakingFieldChecks(player, block, field, event);
            return;
        }
        else if (plugin.getUnbreakableManager().isUnbreakable(block))
        {
            // ------------------------------------------------------------------------------- breaking an unbreakable

            if (plugin.getUnbreakableManager().isOwner(block, player.getName()))
            {
                plugin.getCommunicationManager().notifyDestroyU(player, block);
                plugin.getUnbreakableManager().release(block);
            }
            else if (plugin.getPermissionsManager().has(player, "preciousstones.bypass.unbreakable"))
            {
                plugin.getCommunicationManager().notifyBypassDestroyU(player, block);
                plugin.getUnbreakableManager().release(block);
            }
            else
            {
                event.setCancelled(true);
                plugin.getCommunicationManager().warnDestroyU(player, block);
            }
            return;
        }

        // -------------------------------------------------------------------------------- breaking a prevent destroy area

        Field field = plugin.getForceFieldManager().getEnabledSourceField(block.getLocation(), FieldFlag.PREVENT_DESTROY);

        if (field != null)
        {
            boolean allowed = plugin.getForceFieldManager().isApplyToAllowed(field, player.getName());

            if (!allowed || field.hasFlag(FieldFlag.APPLY_TO_ALL))
            {
                if (plugin.getPermissionsManager().has(player, "preciousstones.bypass.destroy"))
                {
                    plugin.getCommunicationManager().notifyBypassDestroy(player, block, field);
                }
                else
                {
                    event.setCancelled(true);
                    plugin.getCommunicationManager().warnDestroyArea(player, block, field);
                    return;
                }
            }
        }

        // -------------------------------------------------------------------------------- breaking a grief revert area

        field = plugin.getForceFieldManager().getEnabledSourceField(block.getLocation(), FieldFlag.GRIEF_REVERT);

        if (field != null)
        {
            boolean allowed = plugin.getForceFieldManager().isApplyToAllowed(field, player.getName());

            if (!allowed || field.hasFlag(FieldFlag.APPLY_TO_ALL))
            {
                if (plugin.getPermissionsManager().has(player, "preciousstones.bypass.destroy") || field.getSettings().canGrief(block.getTypeId()))
                {
                    plugin.getCommunicationManager().notifyBypassDestroy(player, block, field);
                    plugin.getStorageManager().deleteBlockGrief(block);
                    return;
                }
                else
                {
                    if (!plugin.getSettingsManager().isGriefUndoBlackListType(block.getTypeId()))
                    {
                        boolean clear = !field.hasFlag(FieldFlag.GRIEF_REVERT_DROP);

                        plugin.getGriefUndoManager().addBlock(field, block, clear);
                        plugin.getStorageManager().offerGrief(field);

                        if (clear)
                        {
                            event.setCancelled(true);
                        }
                    }
                }
            }
            else
            {
                plugin.getStorageManager().deleteBlockGrief(block);
            }
        }

        // -------------------------------------------------------------------------------- breaking inside a translocator revert area

        field = plugin.getForceFieldManager().getEnabledSourceField(block.getLocation(), FieldFlag.TRANSLOCATOR);

        if (field != null)
        {
            plugin.getStorageManager().deleteTranslocation(field, player, block);
        }

        // --------------------------------------------------------------------------------

        if (plugin.getSettingsManager().isDebug())
        {
            dt.logProcessTime();
        }
    }

    private boolean breakingFieldChecks(Player player, Block block, Field field, Cancellable event)
    {
        // cancel cuboid if still drawing it

        if (plugin.getCuboidManager().isOpenCuboidField(player, block))
        {
            plugin.getCuboidManager().cancelOpenCuboid(player, block);
            removeAndRefundBlock(player, block, field, event);
            return false;
        }

        boolean isLiquid = block.getTypeId() != field.getTypeId();

        boolean release = false;

        if (field.isOwner(player.getName()))
        {
            if (isLiquid)
            {
                plugin.getCommunicationManager().notifyDestroyFFLiquid(player, field);
            }
            else
            {
                plugin.getCommunicationManager().notifyDestroyFF(player, block);
            }
            release = true;
        }
        else if (field.hasFlag(FieldFlag.BREAKABLE))
        {
            plugin.getCommunicationManager().notifyDestroyBreakableFF(player, block);
            release = true;
        }
        else if (field.hasFlag(FieldFlag.ALLOWED_CAN_BREAK))
        {
            if (plugin.getForceFieldManager().isAllowed(block, player.getName()))
            {
                plugin.getCommunicationManager().notifyDestroyOthersFF(player, block);
                release = true;
            }
        }
        else if (plugin.getPermissionsManager().has(player, "preciousstones.bypass.forcefield"))
        {
            plugin.getCommunicationManager().notifyBypassDestroyFF(player, block);
            release = true;
        }
        else
        {
            plugin.getCommunicationManager().warnDestroyFF(player, block);
            event.setCancelled(true);
        }

        if (plugin.getForceFieldManager().hasSubFields(field))
        {
            ChatBlock.sendMessage(player, ChatColor.RED + "Cannot remove fields that have plot-fields inside of it.  You must remove them first before you can remove this field.");
            event.setCancelled(true);
            return false;
        }

        if (release)
        {
            removeAndRefundBlock(player, block, field, event);
        }

        return release;
    }

    private void removeAndRefundBlock(Player player, Block block, Field field, Cancellable event)
    {
        PreciousStones.debug("releasing field");

        if (block.getTypeId() == field.getTypeId())
        {
            if (plugin.getSettingsManager().isFragileBlock(block))
            {
                PreciousStones.debug("fragile block broken");
                event.setCancelled(true);
                plugin.getForceFieldManager().release(block);
            }
            else
            {
                PreciousStones.debug("silent break");
                plugin.getForceFieldManager().silentRelease(block);
                event.setCancelled(false);
            }
        }
        else
        {
            if (plugin.getSettingsManager().isFragileBlock(new BlockTypeEntry(field.getTypeId(), field.getData())))
            {
                PreciousStones.debug("fragile block broken");
                plugin.getForceFieldManager().releaseLiquid(field);
            }
            else
            {
                PreciousStones.debug("silent break");
                plugin.getForceFieldManager().silentRelease(block);
            }
        }

        if (!plugin.getPermissionsManager().has(player, "preciousstones.bypass.purchase"))
        {
            if (!plugin.getSettingsManager().isNoRefunds())
            {
                if (field.getSettings().getPrice() > 0)
                {
                    // refund the block, account for parent/child relationships

                    if (field.isChild() || field.isParent())
                    {
                        Field parent = field;

                        if (field.isChild())
                        {
                            parent = field.getParent();
                        }

                        plugin.getForceFieldManager().refund(player, parent.getSettings().getPrice());

                        for (Field child : parent.getChildren())
                        {
                            plugin.getForceFieldManager().refund(player, child.getSettings().getPrice());
                        }
                    }
                    else
                    {
                        plugin.getForceFieldManager().refund(player, field.getSettings().getPrice());
                    }
                }
            }
        }
    }

    /**
     * @param event
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onBlockPlace(BlockPlaceEvent event)
    {
        Block block = event.getBlock();
        Player player = event.getPlayer();

        if (block == null || player == null)
        {
            return;
        }

        if (event.isCancelled())
        {
            return;
        }

        if (plugin.getSettingsManager().isBlacklistedWorld(block.getWorld()))
        {
            return;
        }

        if (plugin.getSettingsManager().isBypassBlock(block))
        {
            return;
        }

        DebugTimer dt = new DebugTimer("onBlockPlace");


        // -------------------------------------------------------------------------------------- placing blocks in place of a field

        BlockState state = event.getBlockReplacedState();

        Field existingField = plugin.getForceFieldManager().getField(state.getLocation(), state.getTypeId(), state.getRawData());

        if (existingField != null)
        {
            if (state.getTypeId() > 0)
            {
                if (!breakingFieldChecks(player, block, existingField, event))
                {
                    event.setCancelled(true);
                }
                return;
            }
        }

        // -------------------------------------------------------------------------------------- placing blocks touching a field block that you don't own


        Block fieldBlock = plugin.getForceFieldManager().touchingFieldBlock(block);

        if (fieldBlock != null)
        {
            Field field = plugin.getForceFieldManager().getField(fieldBlock);

            if (field.hasFlag(FieldFlag.ENABLE_WITH_REDSTONE))
            {
                boolean allowed = plugin.getForceFieldManager().isApplyToAllowed(field, player.getName());

                if (!allowed)
                {
                    ChatBlock.sendMessage(player, ChatColor.RED + "Cannot place blocks next to a redstone-enabled field you don't own");
                    event.setCancelled(true);
                    return;
                }
            }
        }

        // -------------------------------------------------------------------------------------- placing with an open cuboid

        if (plugin.getCuboidManager().hasOpenCuboid(player))
        {
            if (!plugin.getSettingsManager().isFieldType(block))
            {
                event.setCancelled(true);
                return;
            }
            else
            {
                CuboidEntry ce = plugin.getCuboidManager().getOpenCuboid(player);
                FieldSettings fs = plugin.getSettingsManager().getFieldSettings(block);

                if (ce.getField().getSettings().getMixingGroup() != fs.getMixingGroup())
                {
                    event.setCancelled(true);
                    ChatBlock.sendMessage(player, ChatColor.RED + "The field type does not mix");
                    return;
                }
            }
        }

        // -------------------------------------------------------------------------------------- prevent place everywhere

        boolean isDisabled = plugin.getPlayerManager().getPlayerEntry(player.getName()).isDisabled();

        if (plugin.getSettingsManager().isPreventPlaceEverywhere() && !plugin.getPermissionsManager().has(player, "preciousstones.bypass.place"))
        {
            boolean isAllowBlock = false;

            if (plugin.getSettingsManager().isFieldType(block) && plugin.getSettingsManager().getFieldSettings(block) != null)
            {
                FieldSettings fs = plugin.getSettingsManager().getFieldSettings(block);

                if (fs.hasDefaultFlag(FieldFlag.ALLOW_DESTROY) || fs.hasDefaultFlag(FieldFlag.ALLOW_PLACE))
                {
                    isAllowBlock = true;
                }
            }

            if (!isAllowBlock)
            {
                Field field = plugin.getForceFieldManager().getEnabledSourceField(block.getLocation(), FieldFlag.ALLOW_PLACE);

                if (field != null)
                {
                    boolean allowed = plugin.getForceFieldManager().isApplyToAllowed(field, player.getName());

                    if (!allowed && !field.hasFlag(FieldFlag.APPLY_TO_ALL))
                    {
                        event.setCancelled(true);
                        return;
                    }
                }
                else
                {
                    event.setCancelled(true);
                    return;
                }
            }
        }

        // -------------------------------------------------------------------------------------- placing a field

        // allow place field if sneaking

        if (plugin.getSettingsManager().isSneakPlaceFields())
        {
            if (player.isSneaking())
            {
                isDisabled = false;
            }
        }

        // bypass place field if sneaking

        if (plugin.getSettingsManager().isSneakNormalBlock())
        {
            if (player.isSneaking())
            {
                isDisabled = true;
            }
        }

        // allow or bypass field placement based on sneak-to-place flag

        FieldSettings settings = plugin.getSettingsManager().getFieldSettings(block);

        if (settings != null)
        {
            if (settings.hasDefaultFlag(FieldFlag.SNEAK_TO_PLACE))
            {
                if (player.isSneaking())
                {
                    isDisabled = false;
                }
                else
                {
                    isDisabled = true;
                }
            }
        }

        if (!isDisabled && plugin.getSettingsManager().isFieldType(block) && plugin.getPermissionsManager().has(player, "preciousstones.benefit.create.forcefield"))
        {
            if (placingFieldChecks(player, block, event))
            {
                // *** ADD THE FIELD

                Field field = plugin.getForceFieldManager().add(block, player, event);

                if (field != null)
                {
                    //field.generateFence();

                    // disable flags

                    if (plugin.getSettingsManager().isStartMessagesDisabled())
                    {
                        field.disableFlag("welcome-message");
                        field.disableFlag("farewell-message");
                    }

                    if (field.hasFlag(FieldFlag.DYNMAP_DISABLED) || plugin.getSettingsManager().isStartDynmapFlagsDisabled())
                    {
                        field.disableFlag("dynmap-area");
                        field.disableFlag("dynmap-marker");
                    }

                    // places the field in a disabled state

                    if (field.hasFlag(FieldFlag.PLACE_DISABLED))
                    {
                        field.setDisabled(true);
                    }

                    // sets the initial state of redstone enabled fields

                    /*
                    if (field.hasFlag(FieldFlag.ENABLE_WITH_REDSTONE))
                    {
                        if (!plugin.getForceFieldManager().isAnywayPowered(block))
                        {
                            field.setDisabled(true);
                        }
                    }
                    */

                    plugin.getStorageManager().offerField(field);

                    // allow all owners of overlapping fields into the field

                    plugin.getForceFieldManager().addAllowOverlappingOwners(field);

                    // start disabling process for auto-disable fields

                    field.startDisabler();
                    return;
                }
            }
        }

        // -------------------------------------------------------------------------------------- placing an unbreakable

        else if (!isDisabled && plugin.getSettingsManager().isUnbreakableType(block) && plugin.getPermissionsManager().has(player, "preciousstones.benefit.create.unbreakable"))
        {
            Field conflictField = plugin.getForceFieldManager().unbreakableConflicts(block, player);

            if (conflictField != null)
            {
                if (plugin.getPermissionsManager().has(player, "preciousstones.bypass.place"))
                {
                    if (plugin.getUnbreakableManager().add(block, player))
                    {
                        plugin.getCommunicationManager().notifyBypassPlaceU(player, block, conflictField);
                    }
                }
                else
                {
                    event.setCancelled(true);
                    plugin.getCommunicationManager().warnConflictU(player, block, conflictField);
                }
            }
            else
            {
                if (plugin.getUnprotectableManager().touchingUnprotectableBlock(block))
                {
                    if (plugin.getPermissionsManager().has(player, "preciousstones.bypass.unprotectable"))
                    {
                        if (plugin.getUnbreakableManager().add(block, player))
                        {
                            plugin.getCommunicationManager().notifyBypassTouchingUnprotectable(player, block);
                        }
                    }
                    else
                    {
                        event.setCancelled(true);
                        plugin.getCommunicationManager().warnUnbreakablePlaceTouchingUnprotectable(player, block);
                    }
                }
                else
                {
                    if (plugin.getUnbreakableManager().add(block, player))
                    {
                        plugin.getCommunicationManager().notifyPlaceU(player, block);
                    }
                }
            }
            return;
        }

        // -------------------------------------------------------------------------------------- placing an unprotectable

        if (plugin.getSettingsManager().isUnprotectableType(block))
        {
            Block unbreakableblock = plugin.getUnbreakableManager().touchingUnbrakableBlock(block);

            if (unbreakableblock != null)
            {
                if (plugin.getPermissionsManager().has(player, "preciousstones.bypass.unprotectable"))
                {
                    plugin.getCommunicationManager().notifyUnbreakableBypassUnprotectableTouching(player, block, unbreakableblock);
                }
                else
                {
                    event.setCancelled(true);
                    plugin.getCommunicationManager().warnUnbreakablePlaceUnprotectableTouching(player, block, unbreakableblock);
                    return;
                }
            }

            Block fieldblock = plugin.getForceFieldManager().touchingFieldBlock(block);

            if (fieldblock != null)
            {
                if (plugin.getPermissionsManager().has(player, "preciousstones.bypass.unprotectable"))
                {
                    plugin.getCommunicationManager().notifyFieldBypassUnprotectableTouching(player, block, fieldblock);
                }
                else
                {
                    event.setCancelled(true);
                    plugin.getCommunicationManager().warnFieldPlaceUnprotectableTouching(player, block, fieldblock);
                    return;
                }
            }

            Field field = plugin.getForceFieldManager().getEnabledSourceField(block.getLocation(), FieldFlag.PREVENT_UNPROTECTABLE);

            if (field != null)
            {
                if (plugin.getPermissionsManager().has(player, "preciousstones.bypass.unprotectable"))
                {
                    plugin.getCommunicationManager().notifyBypassPlaceUnprotectableInField(player, block, field);
                }
                else
                {
                    event.setCancelled(true);
                    plugin.getCommunicationManager().warnPlaceUnprotectableInField(player, block, field);
                }
            }

            field = plugin.getForceFieldManager().getEnabledSourceField(block.getLocation(), FieldFlag.TRANSLOCATOR);

            if (field != null)
            {
                ChatBlock.sendMessage(player, ChatColor.RED + "Cannot place unbreakable blocks inside translocator fields");
                event.setCancelled(true);
            }
        }

        // ------------------------------------------------------------------------------------------- placing a chest next to a field

        if (block.getType().equals(Material.CHEST))
        {
            boolean conflicted = false;

            if (block.getRelative(BlockFace.EAST).getType().equals(Material.CHEST))
            {
                Field field1 = plugin.getForceFieldManager().getConflictSourceField(block.getRelative(BlockFace.EAST).getLocation(), player.getName(), FieldFlag.ALL);

                if (field1 != null)
                {
                    boolean allowed = plugin.getForceFieldManager().isApplyToAllowed(field1, player.getName());

                    if (!allowed || field1.hasFlag(FieldFlag.APPLY_TO_ALL))
                    {
                        conflicted = true;
                    }
                }
            }

            if (block.getRelative(BlockFace.WEST).getType().equals(Material.CHEST))
            {
                Field field2 = plugin.getForceFieldManager().getConflictSourceField(block.getRelative(BlockFace.WEST).getLocation(), player.getName(), FieldFlag.ALL);

                if (field2 != null)
                {
                    boolean allowed = plugin.getForceFieldManager().isApplyToAllowed(field2, player.getName());

                    if (!allowed || field2.hasFlag(FieldFlag.APPLY_TO_ALL))
                    {
                        conflicted = true;
                    }
                }
            }

            if (block.getRelative(BlockFace.NORTH).getType().equals(Material.CHEST))
            {
                Field field3 = plugin.getForceFieldManager().getConflictSourceField(block.getRelative(BlockFace.NORTH).getLocation(), player.getName(), FieldFlag.ALL);

                if (field3 != null)
                {
                    boolean allowed = plugin.getForceFieldManager().isApplyToAllowed(field3, player.getName());

                    if (!allowed || field3.hasFlag(FieldFlag.APPLY_TO_ALL))
                    {
                        conflicted = true;
                    }
                }
            }

            if (block.getRelative(BlockFace.SOUTH).getType().equals(Material.CHEST))
            {
                Field field4 = plugin.getForceFieldManager().getConflictSourceField(block.getRelative(BlockFace.SOUTH).getLocation(), player.getName(), FieldFlag.ALL);

                if (field4 != null)
                {
                    boolean allowed = plugin.getForceFieldManager().isApplyToAllowed(field4, player.getName());

                    if (!allowed || field4.hasFlag(FieldFlag.APPLY_TO_ALL))
                    {
                        conflicted = true;
                    }
                }
            }

            if (conflicted)
            {
                ChatBlock.sendMessage(player, ChatColor.RED + "Cannot place chest next so someone else's field");
                event.setCancelled(true);
                return;
            }
        }

        // -------------------------------------------------------------------------------------- placing in a prevent place area

        Field field = plugin.getForceFieldManager().getEnabledSourceField(block.getLocation(), FieldFlag.PREVENT_PLACE);

        if (field != null)
        {
            boolean allowed = plugin.getForceFieldManager().isApplyToAllowed(field, player.getName());

            if (!allowed || field.hasFlag(FieldFlag.APPLY_TO_ALL))
            {
                if (plugin.getPermissionsManager().has(player, "preciousstones.bypass.place"))
                {
                    plugin.getCommunicationManager().notifyBypassPlace(player, block, field);
                }
                else
                {
                    event.setCancelled(true);
                    plugin.getCommunicationManager().warnPlace(player, block, field);
                }
            }
        }

        // -------------------------------------------------------------------------------------- placing in a grief revert area

        field = plugin.getForceFieldManager().getEnabledSourceField(block.getLocation(), FieldFlag.GRIEF_REVERT);

        if (field != null)
        {
            boolean allowed = plugin.getForceFieldManager().isApplyToAllowed(field, player.getName());

            if (!allowed || field.hasFlag(FieldFlag.APPLY_TO_ALL))
            {
                if (field.hasFlag(FieldFlag.PLACE_GRIEF))
                {
                    if (!plugin.getSettingsManager().isGriefUndoBlackListType(block.getTypeId()))
                    {
                        BlockState blockState = event.getBlockReplacedState();

                        plugin.getGriefUndoManager().addBlock(field, blockState);
                        plugin.getStorageManager().offerGrief(field);
                    }
                }
                else
                {
                    if (plugin.getPermissionsManager().has(player, "preciousstones.bypass.place"))
                    {
                        plugin.getCommunicationManager().notifyBypassPlace(player, block, field);
                        plugin.getStorageManager().deleteBlockGrief(block);
                    }
                    else
                    {
                        event.setCancelled(true);
                        plugin.getCommunicationManager().warnPlace(player, block, field);
                    }
                }
            }
            else
            {
                plugin.getStorageManager().deleteBlockGrief(block);
            }
        }

        // -------------------------------------------------------------------------------------- placing in a translocator area

        field = plugin.getForceFieldManager().getEnabledSourceField(block.getLocation(), FieldFlag.TRANSLOCATOR);

        if (field != null)
        {
            boolean allowed = plugin.getForceFieldManager().isApplyToAllowed(field, player.getName());

            if (allowed || field.hasFlag(FieldFlag.APPLY_TO_ALL))
            {
                if (field.getSettings().canTranslocate(new BlockTypeEntry(block)))
                {
                    plugin.getTranslocationManager().addBlock(field, block);
                    plugin.getStorageManager().offerTranslocation(field);
                }
            }
        }

        // --------------------------------------------------------------------------------------

        plugin.getSnitchManager().recordSnitchBlockPlace(player, block);

        if (plugin.getSettingsManager().isDebug())
        {
            dt.logProcessTime();
        }
    }

    private boolean placingFieldChecks(Player player, Block block, Cancellable event)
    {
        FieldSettings fs = plugin.getSettingsManager().getFieldSettings(block);

        if (fs == null)
        {
            return false;
        }

        // cannot place a field that conflicts with other fields

        Field conflictField = plugin.getForceFieldManager().fieldConflicts(block, player);

        if (conflictField != null)
        {
            event.setCancelled(true);
            plugin.getCommunicationManager().warnConflictFF(player, block, conflictField);
            return false;
        }

        // cannot place a field touching an unprotectable block

        if (plugin.getUnprotectableManager().touchingUnprotectableBlock(block))
        {
            if (!plugin.getPermissionsManager().has(player, "preciousstones.bypass.unprotectable"))
            {
                plugin.getCommunicationManager().warnFieldPlaceTouchingUnprotectable(player, block);
                event.setCancelled(true);
                return false;
            }

            plugin.getCommunicationManager().notifyBypassTouchingUnprotectable(player, block);
        }

        // cannot place a coniscate field below a field or unbreakable

        if (fs.hasDefaultFlag(FieldFlag.CONFISCATE_ITEMS))
        {
            Block north = block.getRelative(BlockFace.NORTH);

            if (plugin.getForceFieldManager().isField(north))
            {
                ChatBlock.sendMessage(player, ChatColor.RED + "Cannot place a confiscating field below a field block");
                event.setCancelled(true);
                return false;
            }

            if (plugin.getUnbreakableManager().isUnbreakable(north))
            {
                ChatBlock.sendMessage(player, ChatColor.RED + "Cannot place a confiscating field below an unbreakable");
                event.setCancelled(true);
                return false;
            }
        }

        // cannot place a field that contains players inside of it if no-player-lpace flag exists

        if (fs.hasDefaultFlag(FieldFlag.NO_PLAYER_PLACE))
        {
            boolean hasPlayers = plugin.getForceFieldManager().fieldTouchesPlayers(block, player);

            if (hasPlayers)
            {
                ChatBlock.sendMessage(player, ChatColor.RED + "Cannot place field near players");
                event.setCancelled(true);
                return false;
            }
        }

        // cannot place a field that contains unprotectable blocks inside of it if prevent-unprotectable flag exists

        if (fs.hasDefaultFlag(FieldFlag.PREVENT_UNPROTECTABLE))
        {
            Block foundblock = plugin.getUnprotectableManager().existsUnprotectableBlock(block);

            if (foundblock != null)
            {
                if (!plugin.getPermissionsManager().has(player, "preciousstones.bypass.unprotectable"))
                {
                    plugin.getCommunicationManager().warnPlaceFieldInUnprotectable(player, foundblock, block);
                    event.setCancelled(true);
                    return false;
                }

                plugin.getCommunicationManager().notifyBypassFieldInUnprotectable(player, foundblock, block);
            }
        }

        // forester blocks need to be placed in fertile lands

        if (fs.hasDefaultFlag(FieldFlag.FORESTER))
        {
            Block floor = block.getRelative(BlockFace.DOWN);

            if (!fs.isFertileType(floor.getTypeId()) && floor.getTypeId() != fs.getGroundBlock())
            {
                player.sendMessage(ChatColor.AQUA + Helper.capitalize(fs.getTitle()) + " blocks must be placed of fertile land to activate");
                return false;
            }
        }

        // if not allowed in this world then place as regular block

        if (!plugin.getPermissionsManager().has(player, "preciousstones.bypass.world"))
        {
            if (!fs.allowedWorld(block.getWorld()))
            {
                return false;
            }
        }

        // ensure placement of only those with the required permission, fail silently otherwise

        if (fs.getRequiredPermission() != null)
        {
            if (!plugin.getPermissionsManager().has(player, fs.getRequiredPermission()))
            {
                return false;
            }
        }

        // ensure placement inside allowed only fields

        if (!plugin.getPermissionsManager().has(player, "preciousstones.bypass.allowed-only-inside"))
        {
            if (fs.hasAllowedOnlyInside())
            {
                List<Field> fields = plugin.getForceFieldManager().getSourceFields(block.getLocation(), FieldFlag.ALL);

                boolean allowed = false;

                for (Field surroundingField : fields)
                {
                    if (fs.isAllowedOnlyInside(surroundingField))
                    {
                        allowed = true;
                        break;
                    }
                }

                if (!allowed)
                {
                    ChatBlock.sendMessage(player, ChatColor.RED + Helper.capitalize(fs.getTitle()) + " needs to be be placed inside " + fs.getAllowedOnlyInsideString());
                    event.setCancelled(true);
                    return false;
                }
            }
        }

        // ensure placement outside allowed only outside fields

        if (!plugin.getPermissionsManager().has(player, "preciousstones.bypass.allowed-only-outside"))
        {
            if (fs.hasAllowedOnlyOutside())
            {
                List<Field> fields = plugin.getForceFieldManager().getSourceFields(block.getLocation(), FieldFlag.ALL);

                boolean notAllowed = false;

                for (Field surroundingField : fields)
                {
                    if (fs.isAllowedOnlyOutside(surroundingField))
                    {
                        notAllowed = true;
                        break;
                    }
                }

                if (notAllowed)
                {
                    ChatBlock.sendMessage(player, ChatColor.RED + Helper.capitalize(fs.getTitle()) + " needs to be be placed outside " + fs.getAllowedOnlyOutsideString());
                    event.setCancelled(true);
                    return false;
                }
            }
        }

        Field field = plugin.getForceFieldManager().getEnabledSourceField(block.getLocation(), FieldFlag.TRANSLOCATOR);

        if (field != null)
        {
            ChatBlock.sendMessage(player, ChatColor.RED + "Cannot place field blocks inside translocator fields");
            event.setCancelled(true);
            return false;
        }

        return true;
    }

    /**
     * @param event
     */

    @EventHandler(priority = EventPriority.HIGH)
    public void onBlockPistonExtend(BlockPistonExtendEvent event)
    {
        Block piston = event.getBlock();

        if (plugin.getSettingsManager().isBlacklistedWorld(piston.getWorld()))
        {
            return;
        }

        Field pistonField = plugin.getForceFieldManager().getEnabledSourceField(piston.getLocation(), FieldFlag.PREVENT_DESTROY);

        List<Block> blocks = event.getBlocks();

        for (Block block : blocks)
        {
            if (plugin.getSettingsManager().isFieldType(block) && plugin.getForceFieldManager().isField(block))
            {
                event.setCancelled(true);
            }

            if (plugin.getSettingsManager().isUnbreakableType(block) && plugin.getUnbreakableManager().isUnbreakable(block))
            {
                event.setCancelled(true);
            }

            Field blockField = plugin.getForceFieldManager().getEnabledSourceField(block.getLocation(), FieldFlag.PREVENT_DESTROY);

            if (pistonField != null && blockField != null)
            {
                if (blockField.isAllowed(pistonField.getOwner()))
                {
                    return;
                }
            }

            if (blockField != null)
            {
                event.setCancelled(true);
            }
        }
    }

    /**
     * @param event
     */

    @EventHandler(priority = EventPriority.HIGH)
    public void onBlockPistonRetract(BlockPistonRetractEvent event)
    {
        boolean unprotected = false;

        Block piston = event.getBlock();

        if (plugin.getSettingsManager().isBlacklistedWorld(piston.getWorld()))
        {
            return;
        }

        Field field = plugin.getForceFieldManager().getEnabledSourceField(piston.getLocation(), FieldFlag.PREVENT_DESTROY);

        if (field == null)
        {
            unprotected = true;
        }

        // prevent piston from moving a field or unbreakable block

        Block block = piston.getRelative(event.getDirection()).getRelative(event.getDirection());

        if (plugin.getSettingsManager().isFieldType(block) && plugin.getForceFieldManager().isField(block))
        {
            event.setCancelled(true);
        }
        if (plugin.getSettingsManager().isUnbreakableType(block) && plugin.getUnbreakableManager().isUnbreakable(block))
        {
            event.setCancelled(true);
        }

        if (unprotected)
        {
            field = plugin.getForceFieldManager().getEnabledSourceField(block.getLocation(), FieldFlag.PREVENT_DESTROY);

            if (field != null)
            {
                event.setCancelled(true);
            }
        }
    }
}
