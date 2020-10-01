/*
 * Copyright © 2019-2020 L2JOrg
 *
 * This file is part of the L2JOrg project.
 *
 * L2JOrg is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * L2JOrg is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.l2j.gameserver.engine.item;

import io.github.joealisson.primitive.HashIntMap;
import io.github.joealisson.primitive.HashIntSet;
import io.github.joealisson.primitive.IntMap;
import io.github.joealisson.primitive.IntSet;
import org.l2j.commons.database.DatabaseFactory;
import org.l2j.commons.threading.ThreadPool;
import org.l2j.gameserver.Config;
import org.l2j.gameserver.data.database.dao.ItemDAO;
import org.l2j.gameserver.data.database.data.ItemData;
import org.l2j.gameserver.data.database.data.ItemOnGroundData;
import org.l2j.gameserver.data.xml.impl.AugmentationEngine;
import org.l2j.gameserver.data.xml.impl.EnchantItemOptionsData;
import org.l2j.gameserver.data.xml.impl.EnsoulData;
import org.l2j.gameserver.engine.geo.GeoEngine;
import org.l2j.gameserver.engine.skill.api.Skill;
import org.l2j.gameserver.enums.InstanceType;
import org.l2j.gameserver.enums.InventorySlot;
import org.l2j.gameserver.enums.ItemLocation;
import org.l2j.gameserver.enums.ItemSkillType;
import org.l2j.gameserver.idfactory.IdFactory;
import org.l2j.gameserver.instancemanager.CastleManager;
import org.l2j.gameserver.instancemanager.ItemsOnGroundManager;
import org.l2j.gameserver.instancemanager.SiegeGuardManager;
import org.l2j.gameserver.model.DropProtection;
import org.l2j.gameserver.model.Location;
import org.l2j.gameserver.model.VariationInstance;
import org.l2j.gameserver.model.WorldObject;
import org.l2j.gameserver.model.actor.Creature;
import org.l2j.gameserver.model.actor.Summon;
import org.l2j.gameserver.model.actor.instance.Player;
import org.l2j.gameserver.model.conditions.Condition;
import org.l2j.gameserver.model.ensoul.EnsoulOption;
import org.l2j.gameserver.model.entity.Castle;
import org.l2j.gameserver.model.events.EventDispatcher;
import org.l2j.gameserver.model.events.impl.character.player.OnPlayerAugment;
import org.l2j.gameserver.model.events.impl.character.player.OnPlayerItemDrop;
import org.l2j.gameserver.model.events.impl.character.player.OnPlayerItemPickup;
import org.l2j.gameserver.model.events.impl.item.OnItemBypassEvent;
import org.l2j.gameserver.model.events.impl.item.OnItemTalk;
import org.l2j.gameserver.model.holders.ItemSkillHolder;
import org.l2j.gameserver.model.instancezone.Instance;
import org.l2j.gameserver.model.item.*;
import org.l2j.gameserver.model.item.container.Inventory;
import org.l2j.gameserver.model.item.container.WarehouseType;
import org.l2j.gameserver.model.item.type.ActionType;
import org.l2j.gameserver.model.item.type.CrystalType;
import org.l2j.gameserver.model.item.type.EtcItemType;
import org.l2j.gameserver.model.item.type.ItemType;
import org.l2j.gameserver.model.options.EnchantOptions;
import org.l2j.gameserver.model.options.Options;
import org.l2j.gameserver.model.stats.Stat;
import org.l2j.gameserver.network.SystemMessageId;
import org.l2j.gameserver.network.serverpackets.*;
import org.l2j.gameserver.settings.GeneralSettings;
import org.l2j.gameserver.util.GMAudit;
import org.l2j.gameserver.util.GameUtils;
import org.l2j.gameserver.world.World;
import org.l2j.gameserver.world.WorldRegion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.*;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;
import java.util.function.Predicate;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static org.l2j.commons.configuration.Configurator.getSettings;
import static org.l2j.commons.database.DatabaseAccess.getDAO;
import static org.l2j.commons.util.Util.doIfNonNull;

/**
 * @author JoeAlisson
 */
public final class Item extends WorldObject {

    private static final Logger LOGGER = LoggerFactory.getLogger(Item.class);
    private static final Logger LOG_ITEMS = LoggerFactory.getLogger("item");

    public static final int[] DEFAULT_ENCHANT_OPTIONS = new int[]{0, 0, 0};

    private final int itemId;
    private final ItemTemplate template;
    private final ReentrantLock _dbLock = new ReentrantLock();
    private final DropProtection _dropProtection = new DropProtection();
    private final List<Options> _enchantOptions = new ArrayList<>();
    private final IntMap<EnsoulOption> _ensoulOptions = new HashIntMap<>(3);
    private final IntMap<EnsoulOption> _ensoulSpecialOptions = new HashIntMap<>(3);

    private ItemChangeType lastChange = ItemChangeType.MODIFIED;
    private VariationInstance augmentation = null;

    /**
     * ID of the owner
     */
    private int ownerId;
    /**
     * ID of who dropped the item last, used for knownlist
     */
    private int _dropperObjectId = 0;
    /**
     * Quantity of the item
     */
    private long count = 1;
    /**
     * Initial Quantity of the item
     */
    private long _initCount;
    /**
     * Remaining time (in miliseconds)
     */
    private long time;
    /**
     * Quantity of the item can decrease
     */
    private boolean _decrease = false;
    /**
     * Location of the item : Inventory, PaperDoll, WareHouse
     */
    private ItemLocation loc;
    /**
     * Slot where item is stored : Paperdoll slot, inventory order ...
     */
    private int locData;
    /**
     * Level of enchantment of the item
     */
    private int enchantLevel;
    /**
     * Wear Item
     */
    private boolean _wear;

    //@formatter:on
    private long _dropTime;
    private boolean _published = false;
    private boolean _protected;
    private boolean _existsInDb; // if a record exists in DB.
    private boolean _storedInDb; // if DB data is up-to-date.
    private ScheduledFuture<?> itemLootShedule = null;
    private ScheduledFuture<?> _lifeTimeTask;

    /**
     * Constructor of the Item from the objectId and the itemId.
     *
     * @param objectId : int designating the ID of the object in the world
     * @param itemId   : int designating the ID of the item
     */
    public Item(int objectId, int itemId) {
        super(objectId);
        setInstanceType(InstanceType.L2ItemInstance);
        template = ItemEngine.getInstance().getTemplate(itemId);
        if (itemId == 0 || isNull(template)) {
            throw new IllegalArgumentException("itemId is not a valid identifier");
        }
        this.itemId = itemId;
        super.setName(template.getName());
        loc = ItemLocation.VOID;
        _dropTime = 0;
        time = template.getTime() == -1 ? -1 : System.currentTimeMillis() + (template.getTime() * 60 * 1000);
        scheduleLifeTimeTask();
    }

    /**
     * Constructor of the Item from the objetId and the description of the item given by the ItemTemplate.
     *
     * @param objectId : int designating the ID of the object in the world
     * @param template : ItemTemplate containing informations of the item
     */
    Item(int objectId, ItemTemplate template) {
        super(objectId);
        setInstanceType(InstanceType.L2ItemInstance);
        itemId = template.getId();
        this.template = template;
        if (itemId == 0) {
            throw new IllegalArgumentException();
        }
        super.setName(this.template.getName());
        loc = ItemLocation.VOID;
        time = this.template.getTime() == -1 ? -1 : System.currentTimeMillis() + (this.template.getTime() * 60 * 1000);
        scheduleLifeTimeTask();
    }

    /**
     * Constructor overload.<br>
     * Sets the next free object ID in the ID factory.
     *
     * @param itemId the item template ID
     */
    public Item(int itemId) {
        this(IdFactory.getInstance().getNextId(), itemId);
    }

    public Item(ItemOnGroundData data) {
        this(data.getObjectId(), data.getItemId());
        setCount(data.getCount());
        setEnchantLevel(data.getEnchantLevel());
        setDropTime(data.getDropTime());
        setProtected(getDropTime() == -1);
        setSpawned(true);
        setXYZ(data.getX(), data.getY(), data.getZ());
    }

    public Item(ItemData data) {
        this(data.getObjectId(), data.getItemId());
        count = data.getCount();
        ownerId = data.getOwnerId();
        loc = data.getLoc();
        locData = data.getLocData();
        enchantLevel = data.getEnchantLevel();
        time = data.getTime();

        _existsInDb = true;
        _storedInDb = true;

        if(isEquipable()) {
            restoreAugmentation();
            restoreSpecialAbilities();
        }

    }

    /**
     * Remove a Item from the world and send server->client GetItem packets.<BR>
     * <BR>
     * <B><U> Actions</U> :</B><BR>
     * <BR>
     * <li>Send a Server->Client Packet GetItem to player that pick up and its _knowPlayers member</li>
     * <li>Remove the WorldObject from the world</li><BR>
     * <BR>
     * <FONT COLOR=#FF0000><B> <U>Caution</U> : This method DOESN'T REMOVE the object from _allObjects of World </B></FONT><BR>
     * <BR>
     * <B><U> Example of use </U> :</B><BR>
     * <BR>
     * <li>Do Pickup Item : PCInstance and Pet</li><BR>
     * <BR>
     *
     * @param character Character that pick up the item
     */
    public final void pickupMe(Creature character) {
        final WorldRegion oldregion = getWorldRegion();

        // Create a server->client GetItem packet to pick up the Item
        character.broadcastPacket(new GetItem(this, character.getObjectId()));

        synchronized (this) {
            setSpawned(false);
        }

        // if this item is a mercenary ticket, remove the spawns!
        final Castle castle = CastleManager.getInstance().getCastle(this);
        if ((castle != null) && (SiegeGuardManager.getInstance().getSiegeGuardByItem(castle.getId(), getId()) != null)) {
            SiegeGuardManager.getInstance().removeTicket(this);
            ItemsOnGroundManager.getInstance().removeObject(this);
        }

        // outside of synchronized to avoid deadlocks
        // Remove the Item from the world
        World.getInstance().removeVisibleObject(this, oldregion);

        if (GameUtils.isPlayer(character)){
            // Notify to scripts
            EventDispatcher.getInstance().notifyEventAsync(new OnPlayerItemPickup(character.getActingPlayer(), this), getTemplate());
        }
    }

    /**
     * Sets the ownerID of the item
     *
     * @param process   : String Identifier of process triggering this action
     * @param owner_id  : int designating the ID of the owner
     * @param creator   : Player Player requesting the item creation
     * @param reference : Object Object referencing current action like NPC selling item or previous item in transformation
     */
    public void setOwnerId(String process, int owner_id, Player creator, Object reference) {
        setOwnerId(owner_id);

        var generalSettings = getSettings(GeneralSettings.class);
        if (generalSettings.logItems()) {
            if (!generalSettings.smallLogItems() || template.isEquipable() || template.getId() == CommonItem.ADENA) {
                if (enchantLevel > 0) {
                    LOG_ITEMS.info("SETOWNER:" + process // in case of null
                            + ", item " + getObjectId() //
                            + ":+" + enchantLevel //
                            + " " + template.getName() //
                            + "(" + count + "), " //
                            + creator + ", " // in case of null
                            + reference); // in case of null
                } else {
                    LOG_ITEMS.info("SETOWNER:" + process // in case of null
                            + ", item " + getObjectId() //
                            + ":" + template.getName() //
                            + "(" + count + "), " //
                            + creator + ", " // in case of null
                            + reference); // in case of null
                }
            }
        }

        if ((creator != null) && creator.isGM()) {
            String referenceName = "no-reference";
            if (reference instanceof WorldObject) {
                referenceName = (((WorldObject) reference).getName() != null ? ((WorldObject) reference).getName() : "no-name");
            } else if (reference instanceof String) {
                referenceName = (String) reference;
            }
            final String targetName = (creator.getTarget() != null ? creator.getTarget().getName() : "no-target");
            if (getSettings(GeneralSettings.class).auditGM()) {
                GMAudit.auditGMAction(creator.getName() + " [" + creator.getObjectId() + "]", process + "(id: " + itemId + " name: " + getName() + ")", targetName, "WorldObject referencing this action is: " + referenceName);
            }
        }
    }

    /**
     * Returns the ownerID of the item
     *
     * @return int : ownerID of the item
     */
    public int getOwnerId() {
        return ownerId;
    }

    /**
     * Sets the ownerID of the item
     *
     * @param owner_id : int designating the ID of the owner
     */
    public void setOwnerId(int owner_id) {
        if (owner_id == ownerId) {
            return;
        }

        // Remove any inventory skills from the old owner.
        removeSkillsFromOwner();

        ownerId = owner_id;
        _storedInDb = false;

        // Give any inventory skills to the new owner only if the item is in inventory
        // else the skills will be given when location is set to inventory.
        giveSkillsToOwner();
    }

    /**
     * Sets the location of the item.<BR>
     * <BR>
     * <U><I>Remark :</I></U> If loc and loc_data different from database, say datas not up-to-date
     *
     * @param loc      : ItemLocation (enumeration)
     * @param loc_data : int designating the slot where the item is stored or the village for freights
     */
    public void setItemLocation(ItemLocation loc, int loc_data) {
        if ((loc == this.loc) && (loc_data == locData)) {
            return;
        }

        // Remove any inventory skills from the old owner.
        removeSkillsFromOwner();

        this.loc = loc;
        locData = loc_data;
        _storedInDb = false;

        // Give any inventory skills to the new owner only if the item is in inventory
        // else the skills will be given when location is set to inventory.
        giveSkillsToOwner();
    }

    public ItemLocation getItemLocation() {
        return loc;
    }

    /**
     * Sets the location of the item
     *
     * @param loc : ItemLocation (enumeration)
     */
    public void setItemLocation(ItemLocation loc) {
        setItemLocation(loc, 0);
    }

    /**
     * @return Returns the count.
     */
    public long getCount() {
        return count;
    }

    /**
     * Sets the quantity of the item.<BR>
     * <BR>
     *
     * @param count the new count to set
     */
    public void setCount(long count) {
        if (this.count == count) {
            return;
        }

        this.count = count >= -1 ? count : 0;
        _storedInDb = false;
    }

    /**
     * Sets the quantity of the item.<BR>
     * <BR>
     * <U><I>Remark :</I></U> If loc and loc_data different from database, say datas not up-to-date
     *
     * @param process   : String Identifier of process triggering this action
     * @param count     : int
     * @param creator   : Player Player requesting the item creation
     * @param reference : Object Object referencing current action like NPC selling item or previous item in transformation
     */
    public void changeCount(String process, long count, Player creator, Object reference) {
        if (count == 0) {
            return;
        }
        final long old = this.count;
        final long max = itemId == CommonItem.ADENA ? Inventory.MAX_ADENA : Integer.MAX_VALUE;

        if ((count > 0) && (this.count > (max - count))) {
            setCount(max);
        } else {
            setCount(this.count + count);
        }

        if (this.count < 0) {
            setCount(0);
        }

        _storedInDb = false;

        var generalSettings = getSettings(GeneralSettings.class);
        if (generalSettings.logItems() && (process != null)) {
            if (!generalSettings.smallLogItems() || template.isEquipable() || template.getId() == CommonItem.ADENA) {
                if (enchantLevel > 0) {
                    LOG_ITEMS.info("CHANGE:" + String.valueOf(process) // in case of null
                            + ", item " + getObjectId() //
                            + ":+" + enchantLevel //
                            + " " + template.getName() //
                            + "(" + this.count + "), PrevCount(" //
                            + String.valueOf(old) + "), " // in case of null
                            + String.valueOf(creator) + ", " // in case of null
                            + String.valueOf(reference)); // in case of null
                } else {
                    LOG_ITEMS.info("CHANGE:" + String.valueOf(process) // in case of null
                            + ", item " + getObjectId() //
                            + ":" + template.getName() //
                            + "(" + this.count + "), PrevCount(" //
                            + String.valueOf(old) + "), " // in case of null
                            + String.valueOf(creator) + ", " // in case of null
                            + String.valueOf(reference)); // in case of null
                }
            }
        }

        if ((creator != null) && creator.isGM()) {
            String referenceName = "no-reference";
            if (reference instanceof WorldObject) {
                referenceName = (((WorldObject) reference).getName() != null ? ((WorldObject) reference).getName() : "no-name");
            } else if (reference instanceof String) {
                referenceName = (String) reference;
            }
            final String targetName = (creator.getTarget() != null ? creator.getTarget().getName() : "no-target");
            if (getSettings(GeneralSettings.class).auditGM()) {
                GMAudit.auditGMAction(creator.getName() + " [" + creator.getObjectId() + "]", process + "(id: " + itemId + " objId: " + getObjectId() + " name: " + getName() + " count: " + count + ")", targetName, "WorldObject referencing this action is: " + referenceName);
            }
        }
    }

    public void changeCountWithoutTrace(long count, Player creator, Object reference) {
        changeCount(null, count, creator, reference);
    }

    /**
     * Return true if item can be enchanted
     *
     * @return boolean
     */
    public boolean isEnchantable() {
        if ((loc == ItemLocation.INVENTORY) || (loc == ItemLocation.PAPERDOLL)) {
            return template.isEnchantable();
        }
        return false;
    }

    /**
     * Returns if item is equipable
     *
     * @return boolean
     */
    public boolean isEquipable() {
        return template instanceof EquipableItem;
    }

    /**
     * Returns if item is equipped
     *
     * @return boolean
     */
    public boolean isEquipped() {
        return (loc == ItemLocation.PAPERDOLL) || (loc == ItemLocation.PET_EQUIP);
    }

    /**
     * Returns the slot where the item is stored
     *
     * @return int
     */
    public int getLocationSlot() {
        return locData;
    }

    /**
     * Returns the characteristics of the item
     *
     * @return ItemTemplate
     * @Deprecated
     */
    public ItemTemplate getTemplate() {
        return template;
    }

    public int getCustomType1() {
        return template.getType1();
    }

    public int getType2() {
        return template.getType2();
    }

    public long getDropTime() {
        return _dropTime;
    }

    public void setDropTime(long time) {
        _dropTime = time;
    }

    /**
     * @return the type of item.
     */
    public ItemType getItemType() {
        return template.getItemType();
    }

    /**
     * Gets the item ID.
     *
     * @return the item ID
     */
    @Override
    public int getId() {
        return itemId;
    }

    /**
     * @return the display Id of the item.
     */
    public int getDisplayId() {
        return template.getDisplayId();
    }

    /**
     * @return {@code true} if item is an EtcItem, {@code false} otherwise.
     */
    public boolean isEtcItem() {
        return (template instanceof EtcItem);
    }

    /**
     * @return {@code true} if item is a Weapon/Shield, {@code false} otherwise.
     */
    public boolean isWeapon() {
        return (template instanceof Weapon);
    }

    /**
     * @return {@code true} if item is an Armor, {@code false} otherwise.
     */
    public boolean isArmor() {
        return (template instanceof Armor);
    }

    /**
     * @return the characteristics of the EtcItem, {@code false} otherwise.
     */
    public EtcItem getEtcItem() {
        if (template instanceof EtcItem) {
            return (EtcItem) template;
        }
        return null;
    }

    /**
     * @return the characteristics of the Weapon.
     */
    public Weapon getWeaponItem() {
        if (template instanceof Weapon) {
            return (Weapon) template;
        }
        return null;
    }

    /**
     * @return the quantity of crystals for crystallization.
     */
    public final int getCrystalCount() {
        return template.getCrystalCount(enchantLevel);
    }

    /**
     * @return the reference price of the item.
     */
    public long getReferencePrice() {
        return template.getReferencePrice();
    }

    /**
     * @return the name of the item.
     */
    public String getItemName() {
        return template.getName();
    }

    /**
     * @return the reuse delay of this item.
     */
    public int getReuseDelay() {
        return template.getReuseDelay();
    }

    /**
     * @return the shared reuse item group.
     */
    public int getSharedReuseGroup() {
        return template.getSharedReuseGroup();
    }

    public ItemChangeType getLastChange() {
        return lastChange;
    }

    public void setLastChange(ItemChangeType lastChange) {
        this.lastChange = lastChange;
    }

    /**
     * Returns if item is stackable
     *
     * @return boolean
     */
    public boolean isStackable() {
        return template.isStackable();
    }

    /**
     * Returns if item is dropable
     *
     * @return boolean
     */
    public boolean isDropable() {
        return !isAugmented() && template.isDropable();
    }

    /**
     * Returns if item is destroyable
     *
     * @return boolean
     */
    public boolean isDestroyable() {
        return template.isDestroyable();
    }

    /**
     * Returns if item is tradeable
     *
     * @return boolean
     */
    public boolean isTradeable() {
        return !isAugmented() && template.isTradeable();
    }

    /**
     * Returns if item is sellable
     *
     * @return boolean
     */
    public boolean isSellable() {
        return !isAugmented() && template.isSellable();
    }

    /**
     * @return if item can be deposited in warehouse or freight
     */
    public boolean isDepositable(boolean isPrivateWareHouse) {
        if (isEquipped() || !template.isDepositable()) {
            return false;
        }
        if (!isPrivateWareHouse) {
            return isTradeable();
        }

        return true;
    }

    public boolean isDepositable(WarehouseType type) {
        if(isEquipped() || !template.isDepositable() ) {
            return false;
        }

        return switch (type) {
            case CLAN, CASTLE -> isTradeable();
            case FREIGHT -> isFreightable();
            default -> true;
        };
    }

    public boolean isPotion() {
        return template.isPotion();
    }

    public boolean isElixir() {
        return template.isElixir();
    }

    public boolean isScroll() {
        return template.isScroll();
    }

    public boolean isHeroItem() {
        return template.isHeroItem();
    }

    public boolean isCommonItem() {
        return template.isCommon();
    }

    /**
     * Returns whether this item is pvp or not
     *
     * @return boolean
     */
    public boolean isPvp() {
        return template.isPvpItem();
    }

    /**
     * @return if item is available for manipulation
     */
    public boolean isAvailable(Player player, boolean allowAdena, boolean allowNonTradeable) {
        final Summon pet = player.getPet();

        return !isEquipped() && !isQuestItem()
                && ( (template.getType2() != ItemTemplate.TYPE2_MONEY) || (template.getType1() != ItemTemplate.TYPE1_SHIELD_ARMOR)) // not money, not shield
                && ((pet == null) || (getObjectId() != pet.getControlObjectId())) // Not Control item of currently summoned pet
                && !player.isProcessingItem(getObjectId()) // Not momentarily used enchant scroll
                && (allowAdena || (itemId != CommonItem.ADENA)) // Not Adena
                && (!player.isCastingNow(s -> s.getSkill().getItemConsumeId() != itemId))
                && (allowNonTradeable || (isTradeable() && (!((template.getItemType() == EtcItemType.PET_COLLAR) && player.havePetInvItems()))));
    }

    /**
     * Returns the level of enchantment of the item
     *
     * @return int
     */
    public int getEnchantLevel() {
        return enchantLevel;
    }

    public void updateEnchantLevel(int value) {
        setEnchantLevel(enchantLevel + value);
    }

    /**
     * @param enchantLevel the enchant value to set
     */
    public void setEnchantLevel(int enchantLevel) {
        if (this.enchantLevel == enchantLevel) {
            return;
        }
        clearEnchantStats();
        this.enchantLevel = enchantLevel;
        applyEnchantStats();
        _storedInDb = false;
    }

    /**
     * @return {@code true} if item is enchanted, {@code false} otherwise
     */
    public boolean isEnchanted() {
        return enchantLevel > 0;
    }

    /**
     * Returns whether this item is augmented or not
     *
     * @return true if augmented
     */
    public boolean isAugmented() {
        return nonNull(augmentation);
    }

    public void applyAugmentationBonus(Player player) {
        if(nonNull(augmentation)) {
            augmentation.applyBonus(player);
        }
    }

    public void removeAugmentationBonus(Player player) {
        if(nonNull(augmentation)) {
            augmentation.removeBonus(player);
        }
    }

    /**
     * Returns the augmentation object for this item
     *
     * @return augmentation
     */
    public VariationInstance getAugmentation() {
        return augmentation;
    }

    /**
     * Sets a new augmentation
     *
     * @param augmentation
     * @param updateDatabase
     * @return return true if successfully
     */
    public boolean setAugmentation(VariationInstance augmentation, boolean updateDatabase) {
        // there shall be no previous augmentation..
        if (this.augmentation != null) {
            LOGGER.info("Warning: Augment set for (" + getObjectId() + ") " + getName() + " owner: " + ownerId);
            return false;
        }

        this.augmentation = augmentation;
        if (updateDatabase) {
            updateItemVariation();
        }
        EventDispatcher.getInstance().notifyEventAsync(new OnPlayerAugment(getActingPlayer(), this, augmentation, true), getTemplate());
        return true;
    }

    /**
     * Remove the augmentation
     */
    public void removeAugmentation() {
        if (augmentation == null) {
            return;
        }

        // Copy augmentation before removing it.
        final VariationInstance augment = augmentation;
        augmentation = null;


        getDAO(ItemDAO.class).deleteVariations(objectId);
        EventDispatcher.getInstance().notifyEventAsync(new OnPlayerAugment(getActingPlayer(), this, augment, false), getTemplate());
    }

    public void restoreAugmentation() {
        doIfNonNull(getDAO(ItemDAO.class).findItemVariationByItem(objectId), data -> augmentation = new VariationInstance(data));
    }

    public void updateItemVariation() {
        if(nonNull(augmentation)) {
            getDAO(ItemDAO.class).save(augmentation.getData());
        } else {
            getDAO(ItemDAO.class).deleteVariations(objectId);
        }
    }

    /**
     * Returns false cause item can't be attacked
     *
     * @return boolean false
     */
    @Override
    public boolean isAutoAttackable(Creature attacker) {
        return false;
    }

    /**
     * Updates the database.<BR>
     */
    public void updateDatabase() {
        updateDatabase(false);
    }

    /**
     * Updates the database.<BR>
     *
     * @param force if the update should necessarilly be done.
     */
    public void updateDatabase(boolean force) {
        _dbLock.lock();

        try {
            if (_existsInDb) {
                if ((ownerId == 0) || (loc == ItemLocation.VOID) || (loc == ItemLocation.REFUND) || ((count == 0) && (loc != ItemLocation.LEASE))) {
                    removeFromDb();
                } else if (!Config.LAZY_ITEMS_UPDATE || force) {
                    updateInDb();
                }
            } else {
                if ((ownerId == 0) || (loc == ItemLocation.VOID) || (loc == ItemLocation.REFUND) || ((count == 0) && (loc != ItemLocation.LEASE))) {
                    return;
                }
                insertIntoDb();
            }
        } finally {
            _dbLock.unlock();
        }
    }

    public final void dropMe(Creature dropper, int x, int y, int z) {
        ThreadPool.execute(new ItemDropTask(this, dropper, x, y, z));
        if (GameUtils.isPlayer(dropper)) {
            // Notify to scripts
            EventDispatcher.getInstance().notifyEventAsync(new OnPlayerItemDrop(dropper.getActingPlayer(), this, new Location(x, y, z)), getTemplate());
        }
    }

    /**
     * Update the database with values of the item
     */
    private void updateInDb() {
        if (!_existsInDb || _wear || _storedInDb) {
            return;
        }

        try (Connection con = DatabaseFactory.getInstance().getConnection();
             PreparedStatement ps = con.prepareStatement("UPDATE items SET owner_id=?,count=?,loc=?,loc_data=?,enchant_level=?,time=? WHERE object_id = ?")) {
            ps.setInt(1, ownerId);
            ps.setLong(2, count);
            ps.setString(3, loc.name());
            ps.setInt(4, locData);
            ps.setInt(5, enchantLevel);
            ps.setLong(6, time);
            ps.setInt(7, getObjectId());
            ps.executeUpdate();
            _existsInDb = true;
            _storedInDb = true;

            if (augmentation != null) {
                updateItemVariation();
            }

            if (!_ensoulOptions.isEmpty() || !_ensoulSpecialOptions.isEmpty()) {
                updateSpecialAbilities(con);
            }
        } catch (Exception e) {
            LOGGER.error("Could not update item " + this + " in DB: Reason: " + e.getMessage(), e);
        }
    }

    /**
     * Insert the item in database
     */
    private void insertIntoDb() {
        if (_existsInDb || (getObjectId() == 0) || _wear) {
            return;
        }

        try (Connection con = DatabaseFactory.getInstance().getConnection();
             PreparedStatement ps = con.prepareStatement("INSERT INTO items (owner_id,item_id,count,loc,loc_data,enchant_level,object_id,time) VALUES (?,?,?,?,?,?,?,?)")) {
            ps.setInt(1, ownerId);
            ps.setInt(2, itemId);
            ps.setLong(3, count);
            ps.setString(4, loc.name());
            ps.setInt(5, locData);
            ps.setInt(6, enchantLevel);
            ps.setInt(7, getObjectId());
            ps.setLong(8, time);

            ps.executeUpdate();
            _existsInDb = true;
            _storedInDb = true;

            if (augmentation != null) {
                updateItemVariation();
            }

            if ((_ensoulOptions != null) || (_ensoulSpecialOptions != null)) {
                updateSpecialAbilities(con);
            }
        } catch (Exception e) {
            LOGGER.error("Could not insert item " + this + " into DB: Reason: " + e.getMessage(), e);
        }
    }

    /**
     * Delete item from database
     */
    private void removeFromDb() {
        if (!_existsInDb || _wear) {
            return;
        }

        getDAO(ItemDAO.class).deleteItem(objectId);
        _existsInDb = false;
        _storedInDb = false;
    }

    /**
     * Returns the item in String format
     *
     * @return String
     */
    @Override
    public String toString() {
        return template + "[" + getObjectId() + "]";
    }

    public void resetOwnerTimer() {
        if (itemLootShedule != null) {
            itemLootShedule.cancel(true);
            itemLootShedule = null;
        }
    }

    public ScheduledFuture<?> getItemLootShedule() {
        return itemLootShedule;
    }

    public void setItemLootShedule(ScheduledFuture<?> sf) {
        itemLootShedule = sf;
    }

    public boolean isProtected() {
        return _protected;
    }

    public void setProtected(boolean isProtected) {
        _protected = isProtected;
    }

    public boolean isAvailable() {
        if (!template.isConditionAttached()) {
            return true;
        }
        if ((loc == ItemLocation.PET) || (loc == ItemLocation.PET_EQUIP))
        {
            return true;
        }
        Creature owner = getActingPlayer();
        if (owner != null) {
            for (Condition condition : template.getConditions()) {
                if (condition == null) {
                    continue;
                }
                try {
                    if (!condition.test(owner, owner, null, null)) {
                        return false;
                    }
                }
                catch (Exception e)
                {
                }
            }
        }
        return true;
    }

    public boolean getCountDecrease() {
        return _decrease;
    }

    public void setCountDecrease(boolean decrease) {
        _decrease = decrease;
    }

    public long getInitCount() {
        return _initCount;
    }

    public void setInitCount(int InitCount) {
        _initCount = InitCount;
    }

    public void restoreInitCount() {
        if (_decrease) {
            setCount(_initCount);
        }
    }

    public boolean isTimeLimitedItem() {
        return time > 0;
    }

    /**
     * Returns (current system time + time) of this time limited item
     *
     * @return Time
     */
    public long getTime() {
        return time;
    }

    public long getRemainingTime() {
        return time - System.currentTimeMillis();
    }

    public void endOfLife() {
        final Player player = getActingPlayer();
        if (player != null) {
            if (isEquipped()) {
                var unequiped = player.getInventory().unEquipItemInSlotAndRecord(InventorySlot.fromId(getLocationSlot()));
                final InventoryUpdate iu = new InventoryUpdate();
                for (Item item : unequiped) {
                    iu.addModifiedItem(item);
                }
                player.sendInventoryUpdate(iu);
            }

            if (loc != ItemLocation.WAREHOUSE) {
                // destroy
                player.getInventory().destroyItem("Item", this, player, null);

                // send update
                final InventoryUpdate iu = new InventoryUpdate();
                iu.addRemovedItem(this);
                player.sendInventoryUpdate(iu);
            } else {
                player.getWarehouse().destroyItem("Item", this, player, null);
            }
            player.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.S1_HAS_EXPIRED).addItemName(itemId));
        }
    }

    public void scheduleLifeTimeTask() {
        if (!isTimeLimitedItem()) {
            return;
        }

        if (getRemainingTime() <= 0) {
            endOfLife();
        } else {
            if (_lifeTimeTask != null) {
                _lifeTimeTask.cancel(true);
            }
            _lifeTimeTask = ThreadPool.schedule(new ScheduleLifeTimeTask(this), getRemainingTime());
        }
    }

    public void setDropperObjectId(int id) {
        _dropperObjectId = id;
    }

    @Override
    public void sendInfo(Player activeChar) {
        if (_dropperObjectId != 0) {
            activeChar.sendPacket(new DropItem(this, _dropperObjectId));
        } else {
            activeChar.sendPacket(new SpawnItem(this));
        }
    }

    public final DropProtection getDropProtection() {
        return _dropProtection;
    }

    public boolean isPublished() {
        return _published;
    }

    public void publish() {
        _published = true;
    }

    @Override
    public boolean decayMe() {
        if (getSettings(GeneralSettings.class).saveDroppedItems()) {
            ItemsOnGroundManager.getInstance().removeObject(this);
        }

        return super.decayMe();
    }

    public boolean isQuestItem() {
        return template.isQuestItem();
    }

    public boolean isFreightable() {
        return template.isFreightable();
    }

    public boolean hasPassiveSkills() {
        return (template.getItemType() == EtcItemType.RUNE || template.getItemType() == EtcItemType.NONE) && (loc == ItemLocation.INVENTORY) && (ownerId > 0) && (template.getSkills(ItemSkillType.NORMAL) != null);
    }

    public void giveSkillsToOwner() {
        if (!hasPassiveSkills()) {
            return;
        }

        doIfNonNull(getActingPlayer(), player -> {
            template.forEachSkill(ItemSkillType.NORMAL, holder -> {
                final Skill skill = holder.getSkill();
                if (skill.isPassive()) {
                    player.addSkill(skill, false);
                }
            });
        });
    }

    public void removeSkillsFromOwner() {
        if (!hasPassiveSkills()) {
            return;
        }

        doIfNonNull(getActingPlayer(), player -> {

            IntSet removedSkills = new HashIntSet();
            template.forEachSkill(ItemSkillType.NORMAL, Skill::isPassive, skill -> {
                var oldSkill = player.removeSkill(skill, false, true);
                if(nonNull(oldSkill)) {
                    removedSkills.add(oldSkill.getId());
                }
            });

            if(!removedSkills.isEmpty()) {
                player.getInventory().forEachItem(hasRemovedSkill(removedSkills), Item::giveSkillsToOwner);
            }
        });
    }

    public Predicate<Item> hasRemovedSkill(IntSet removedSkills) {
        return item -> item != this && item.hasPassiveSkills() && item.getTemplate().checkAnySkill(ItemSkillType.NORMAL, sk -> removedSkills.contains(sk.getSkillId()) );
    }

    @Override
    public Player getActingPlayer() {
        return World.getInstance().findPlayer(getOwnerId());
    }

    public int getEquipReuseDelay() {
        return template.getEquipReuseDelay();
    }

    public void onBypassFeedback(Player activeChar, String command) {
        if (command.startsWith("Quest")) {
            final String questName = command.substring(6);
            String event = null;
            final int idx = questName.indexOf(' ');
            if (idx > 0) {
                event = questName.substring(idx).trim();
            }

            if (event != null) {
                EventDispatcher.getInstance().notifyEventAsync(new OnItemBypassEvent(this, activeChar, event), getTemplate());
            } else {
                EventDispatcher.getInstance().notifyEventAsync(new OnItemTalk(this, activeChar), getTemplate());
            }
        }
    }

    /**
     * Returns enchant effect object for this item
     *
     * @return enchanteffect
     */
    public int[] getEnchantOptions() {
        final EnchantOptions op = EnchantItemOptionsData.getInstance().getOptions(this);
        if (op != null) {
            return op.getOptions();
        }
        return DEFAULT_ENCHANT_OPTIONS;
    }

    public Collection<EnsoulOption> getSpecialAbilities() {
        return Collections.unmodifiableCollection(_ensoulOptions.values());
    }

    public EnsoulOption getSpecialAbility(int index) {
        return _ensoulOptions.get(index);
    }

    public Collection<EnsoulOption> getAdditionalSpecialAbilities() {
        return Collections.unmodifiableCollection(_ensoulSpecialOptions.values());
    }

    public EnsoulOption getAdditionalSpecialAbility(int index) {
        return _ensoulSpecialOptions.get(index);
    }

    public void addSpecialAbility(EnsoulOption option, int position, int type, boolean updateInDB) {
        if (type == 1) // Adding regular ability
        {
            final EnsoulOption oldOption = _ensoulOptions.put(position, option);
            if (oldOption != null) {
                removeSpecialAbility(oldOption);
            }
        } else if (type == 2) // Adding special ability
        {
            final EnsoulOption oldOption = _ensoulSpecialOptions.put(position, option);
            if (oldOption != null) {
                removeSpecialAbility(oldOption);
            }
        }

        if (updateInDB) {
            updateSpecialAbilities();
        }
    }

    public void removeSpecialAbility(int position, int type) {
        if (type == 1) {
            final EnsoulOption option = _ensoulOptions.get(position);
            if (option != null) {
                removeSpecialAbility(option);
                _ensoulOptions.remove(position);
                // Rearrange.
                if (position == 0)
                {
                    final EnsoulOption secondEnsoul = _ensoulOptions.get(1);
                    if (secondEnsoul != null)
                    {
                        removeSpecialAbility(secondEnsoul);
                        _ensoulOptions.remove(1);
                        addSpecialAbility(secondEnsoul, 0, 1, true);
                    }
                }
            }
        } else if (type == 2) {
            final EnsoulOption option = _ensoulSpecialOptions.get(position);
            if (option != null) {
                removeSpecialAbility(option);
                _ensoulSpecialOptions.remove(position);
            }
        }
    }

    public void clearSpecialAbilities() {
        _ensoulOptions.values().forEach(this::clearSpecialAbility);
        _ensoulSpecialOptions.values().forEach(this::clearSpecialAbility);
    }

    public void applySpecialAbilities() {
        if (!isEquipped()) {
            return;
        }

        _ensoulOptions.values().forEach(this::applySpecialAbility);
        _ensoulSpecialOptions.values().forEach(this::applySpecialAbility);
    }

    private void removeSpecialAbility(EnsoulOption option) {
        try (Connection con = DatabaseFactory.getInstance().getConnection();
             PreparedStatement ps = con.prepareStatement("DELETE FROM item_special_abilities WHERE objectId = ? AND optionId = ?")) {
            ps.setInt(1, getObjectId());
            ps.setInt(2, option.getId());
            ps.execute();

            final Skill skill = option.getSkill();
            if (skill != null) {
                final Player player = getActingPlayer();
                if (player != null) {
                    player.removeSkill(skill.getId());
                }
            }
        } catch (Exception e) {
            LOGGER.warn("Couldn't remove special ability for item: " + this, e);
        }
    }

    private void applySpecialAbility(EnsoulOption option) {
        final Skill skill = option.getSkill();
        if (skill != null) {
            final Player player = getActingPlayer();
            if (player != null) {
                if (player.getSkillLevel(skill.getId()) != skill.getLevel()) {
                    player.addSkill(skill, false);
                }
            }
        }
    }

    private void clearSpecialAbility(EnsoulOption option) {
        final Skill skill = option.getSkill();
        if (skill != null) {
            final Player player = getActingPlayer();
            if (player != null) {
                player.removeSkill(skill, false, true);
            }
        }
    }

    private void restoreSpecialAbilities() {
        try (Connection con = DatabaseFactory.getInstance().getConnection();
             PreparedStatement ps = con.prepareStatement("SELECT * FROM item_special_abilities WHERE objectId = ? ORDER BY position")) {
            ps.setInt(1, getObjectId());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    final int optionId = rs.getInt("optionId");
                    final int type = rs.getInt("type");
                    final int position = rs.getInt("position");
                    final EnsoulOption option = EnsoulData.getInstance().getOption(optionId);
                    if (option != null) {
                        addSpecialAbility(option, position, type, false);
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.warn("Couldn't restore special abilities for item: " + this, e);
        }
    }

    public void updateSpecialAbilities() {
        try (Connection con = DatabaseFactory.getInstance().getConnection()) {
            updateSpecialAbilities(con);
        } catch (Exception e) {
            LOGGER.warn("Couldn't update item special abilities", e);
        }
    }

    private void updateSpecialAbilities(Connection con) {
        try (PreparedStatement ps = con.prepareStatement("INSERT INTO item_special_abilities (`objectId`, `type`, `optionId`, `position`) VALUES (?, ?, ?, ?) ON DUPLICATE KEY UPDATE type = ?, optionId = ?, position = ?")) {
            ps.setInt(1, getObjectId());
            for (var entry : _ensoulOptions.entrySet()) {
                ps.setInt(2, 1); // regular options
                ps.setInt(3, entry.getValue().getId());
                ps.setInt(4, entry.getKey());

                ps.setInt(5, 1); // regular options
                ps.setInt(6, entry.getValue().getId());
                ps.setInt(7, entry.getKey());
                ps.execute();
            }

            for (var entry : _ensoulSpecialOptions.entrySet()) {
                ps.setInt(2, 2); // special options
                ps.setInt(3, entry.getValue().getId());
                ps.setInt(4, entry.getKey());

                ps.setInt(5, 2); // special options
                ps.setInt(6, entry.getValue().getId());
                ps.setInt(7, entry.getKey());
                ps.execute();
            }
        } catch (Exception e) {
            LOGGER.warn("Couldn't update item special abilities", e);
        }
    }

    /**
     * Clears all the enchant bonuses if item is enchanted and containing bonuses for enchant value.
     */
    public void clearEnchantStats() {
        final Player player = getActingPlayer();
        if (player == null) {
            _enchantOptions.clear();
            return;
        }

        for (Options op : _enchantOptions) {
            op.remove(player);
        }
        _enchantOptions.clear();
    }

    /**
     * Clears and applies all the enchant bonuses if item is enchanted and containing bonuses for enchant value.
     */
    public void applyEnchantStats() {
        final Player player = getActingPlayer();
        if (!isEquipped() || (player == null) || (getEnchantOptions() == DEFAULT_ENCHANT_OPTIONS)) {
            return;
        }

        for (int id : getEnchantOptions()) {
            final Options options = AugmentationEngine.getInstance().getOptions(id);
            if (options != null) {
                options.apply(player);
                _enchantOptions.add(options);
            } else if (id != 0) {
                LOGGER.info("applyEnchantStats: Couldn't find option: " + id);
            }
        }
    }

    @Override
    public void setHeading(int heading) {
    }

    public void deleteMe() {
        if ((_lifeTimeTask != null) && !_lifeTimeTask.isDone()) {
            _lifeTimeTask.cancel(false);
            _lifeTimeTask = null;
        }
    }

    public BodyPart getBodyPart() {
        return template instanceof EquipableItem ? template.getBodyPart() : BodyPart.NONE;
    }

    public int getItemMask() {
        return template.getItemMask();
    }

    public void forEachSkill(ItemSkillType type, Consumer<ItemSkillHolder> action) {
        template.forEachSkill(type, action);
    }

    public double getStats(Stat stat, int defaultValue) {
        return template.getStats(stat, defaultValue);
    }

    public boolean isAutoPotion() {
        return template instanceof EtcItem && ((EtcItem) template).isAutoPotion();
    }

    public boolean isAutoSupply() {
        return template instanceof EtcItem && ((EtcItem) template).isAutoSupply();
    }

    public ActionType getAction() {
        return template.getDefaultAction();
    }

    public List<ItemSkillHolder> getSkills(ItemSkillType type) {
        return template.getSkills(type);
    }

    public boolean isSelfResurrection() {
        return template instanceof EtcItem etcTemplate && etcTemplate.isSelfResurrection();
    }

    public CrystalType getCrystalType() {
        return template.getCrystalType();
    }

    public boolean isMagicWeapon() {
        return template instanceof Weapon w && w.isMagicWeapon();
    }

    public boolean isInfinite() {
        return template instanceof EtcItem etcItem && etcItem.isInfinite();
    }

    static class ScheduleLifeTimeTask implements Runnable {
        private static final Logger LOGGER = LoggerFactory.getLogger(ScheduleLifeTimeTask.class);
        private final Item _limitedItem;

        ScheduleLifeTimeTask(Item item) {
            _limitedItem = item;
        }

        @Override
        public void run() {
            try {
                if (_limitedItem != null) {
                    _limitedItem.endOfLife();
                }
            } catch (Exception e) {
                LOGGER.error("", e);
            }
        }
    }

    /**
     * Init a dropped Item and add it in the world as a visible object.<BR>
     * <BR>
     * <B><U> Actions</U> :</B><BR>
     * <BR>
     * <li>Set the x,y,z position of the Item dropped and update its _worldregion</li>
     * <li>Add the Item dropped to _visibleObjects of its WorldRegion</li>
     * <li>Add the Item dropped in the world as a <B>visible</B> object</li><BR>
     * <BR>
     * <FONT COLOR=#FF0000><B> <U>Caution</U> : This method DOESN'T ADD the object to _allObjects of World </B></FONT><BR>
     * <BR>
     * <B><U> Example of use </U> :</B><BR>
     * <BR>
     * <li>Drop item</li>
     * <li>Call Pet</li><BR>
     */
    public class ItemDropTask implements Runnable {
        private final Creature _dropper;
        private final Item _item;
        private int _x, _y, _z;

        public ItemDropTask(Item item, Creature dropper, int x, int y, int z) {
            _x = x;
            _y = y;
            _z = z;
            _dropper = dropper;
            _item = item;
        }

        @Override
        public final void run() {
            if (_dropper != null) {
                final Instance instance = _dropper.getInstanceWorld();
                final Location dropDest = GeoEngine.getInstance().canMoveToTargetLoc(_dropper.getX(), _dropper.getY(), _dropper.getZ(), _x, _y, _z, instance);
                _x = dropDest.getX();
                _y = dropDest.getY();
                _z = dropDest.getZ();
                setInstance(instance); // Inherit instancezone when dropped in visible world
            } else {
                setInstance(null); // No dropper? Make it a global item...
            }

            synchronized (_item) {
                // Set the x,y,z position of the Item dropped and update its _worldregion
                _item.setSpawned(true);
                _item.setXYZ(_x, _y, _z);
            }

            _item.setDropTime(System.currentTimeMillis());
            _item.setDropperObjectId(_dropper != null ? _dropper.getObjectId() : 0); // Set the dropper Id for the knownlist packets in sendInfo

            // Add the Item dropped in the world as a visible object
            World.getInstance().addVisibleObject(_item, _item.getWorldRegion());
            if (getSettings(GeneralSettings.class).saveDroppedItems()) {
                ItemsOnGroundManager.getInstance().save(_item);
            }
            _item.setDropperObjectId(0); // Set the dropper Id back to 0 so it no longer shows the drop packet
        }
    }
}