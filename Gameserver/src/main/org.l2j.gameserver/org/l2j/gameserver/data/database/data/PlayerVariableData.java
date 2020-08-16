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
package org.l2j.gameserver.data.database.data;

import org.l2j.commons.database.annotation.Column;
import org.l2j.commons.database.annotation.Table;

/**
 * @author JoeAlisson
 */
@Table("player_variables")
public class PlayerVariableData {

    public static final int REVENGE_USABLE_FUNCTIONS = 5;

    public static PlayerVariableData init(int playerId, byte face, byte hairStyle, byte hairColor) {
        var data = new PlayerVariableData();
        data.revengeTeleports = REVENGE_USABLE_FUNCTIONS;
        data.revengeLocations = REVENGE_USABLE_FUNCTIONS;
        data.playerId = playerId;
        data.visualFaceId = face;
        data.visualHairId = hairStyle;
        data.visualHairColorId = hairColor;
        return data;
    }

    @Column("player_id")
    private int playerId;

    @Column("revenge_teleports")
    private byte revengeTeleports;

    @Column("revenge_locations")
    private byte revengeLocations;

    @Column("hair_accessory_enabled")
    private boolean hairAccessoryEnabled;

    @Column("world_chat_used")
    private int worldChatUsed;

    @Column("vitality_items_used")
    private int vitalityItemsUsed;

    @Column("ability_points_main_class_used")
    private int abilityPointsMainClassUsed;

    @Column("ability_points_dual_class_used")
    private int abilityPointsDualClassUsed;

    @Column("revelation_skill_main_class_1")
    private int revelationSkillMainClass1;

    @Column("revelation_skill_main_class_2")
    private int revelationSkillMainClass2;

    @Column("revelation_skill_dual_class_1")
    private int revelationSkillDualClass1;

    @Column("revelation_skill_dual_class_2")
    private int revelationSkillDualClass2;

    @Column("extend_drop")
    private String extendDrop;

    @Column("fortune_telling")
    private int fortuneTelling;

    @Column("fortune_telling_black_cat")
    private boolean fortuneTellingBlackCat;

    @Column("hunting_zone_reset_time")
    private String huntingZoneResetTime;

    private int autoCp;

    private int autoHp;

    private int autoMp;

    @Column("exp_off")
    private boolean expOff;

    @Column("items_rewarded")
    private boolean itemsRewarded;

    @Column("henna1_duration")
    private long henna1Duration;

    @Column("henna2_duration")
    private long henna2Duration;

    @Column("henna3_duration")
    private long henna3Duration;

    @Column("visual_hair_id")
    private int visualHairId;

    @Column("visual_hair_color_id")
    private int visualHairColorId;

    @Column("visual_face_id")
    private int visualFaceId;

    @Column("instance_origin")
    private String instanceOrigin;

    @Column("instance_restore")
    private int instanceRestore;

    @Column("claimed_clan_rewards")
    private int claimedClanRewards;

    @Column("cond_override_key")
    private String condOverrideKey;

    @Column("ui_key_mapping")
    private String uiKeyMapping;

    @Column("attendance_date")
    private long attendanceDate;

    @Column("attendance_index")
    private int attendanceIndex;

    @Column("unclaimed_olympiad_points")
    private int unclaimedOlympiadPoints;

    @Column("monster_return")
    private int monsterReturn;

    public boolean isHairAccessoryEnabled() {
        return hairAccessoryEnabled;
    }

    public int getWorldChatUsed() {
        return worldChatUsed;
    }

    public int getVitalityItemsUsed() {
        return vitalityItemsUsed;
    }

    public int getAbilityPointsMainClassUsed() {
        return abilityPointsMainClassUsed;
    }

    public int getAbilityPointsDualClassUsed() {
        return abilityPointsDualClassUsed;
    }

    public int getRevelationSkillMainClass1() {
        return revelationSkillMainClass1;
    }

    public int getRevelationSkillMainClass2() {
        return revelationSkillMainClass2;
    }

    public int getRevelationSkillDualClass1() {
        return revelationSkillDualClass1;
    }

    public int getRevelationSkillDualClass2() {
        return revelationSkillDualClass2;
    }

    public String getExtendDrop() {
        return extendDrop;
    }

    public int getFortuneTelling() {
        return fortuneTelling;
    }

    public boolean isFortuneTellingBlackCat() {
        return fortuneTellingBlackCat;
    }

    public String getHuntingZoneResetTime() {
        return huntingZoneResetTime;
    }

    public int getAutoCp() {
        return autoCp;
    }

    public int getAutoHp() {
        return autoHp;
    }

    public int getAutoMp() {
        return autoMp;
    }

    public boolean getExpOff() {
        return expOff;
    }

    public boolean isItemsRewarded() {
        return itemsRewarded;
    }

    public long getHenna1Duration() {
        return henna1Duration;
    }

    public long getHenna2Duration() {
        return henna2Duration;
    }

    public long getHenna3Duration() {
        return henna3Duration;
    }

    public int getVisualHairId() {
        return visualHairId;
    }

    public int getVisualHairColorId() {
        return visualHairColorId;
    }

    public int getVisualFaceId() {
        return visualFaceId;
    }

    public int[] getInstanceOrigin() {
        String[] instanceOriginString = instanceOrigin.split(";");
        int[] instanceOriginInt = new int [instanceOriginString.length];

        for (int i = 0; i < instanceOriginString.length ; i++)
            instanceOriginInt[i] = Integer.parseInt(instanceOriginString[i]);

        return instanceOriginInt;
    }

    public int getInstanceRestore() {
        return instanceRestore;
    }

    public int getClaimedClanRewards() {
        return claimedClanRewards;
    }

    public String getCondOverrideKey() {
        return condOverrideKey;
    }

    public String getUiKeyMapping() {
        return uiKeyMapping;
    }

    public long getAttendanceDate() {
        return attendanceDate;
    }

    public int getAttendanceIndex() {
        return attendanceIndex;
    }

    public int getUnclaimedOlympiadPoints() {
        return unclaimedOlympiadPoints;
    }

    public int getMonsterReturn() {
        return monsterReturn;
    }

    public void setHairAccessoryEnabled(boolean hairAccessoryEnabled) {
        this.hairAccessoryEnabled = hairAccessoryEnabled;
    }

    public void setWorldChatUsed(int worldChatUsed) {
        this.worldChatUsed = worldChatUsed;
    }

    public void setVitalityItemsUsed(int vitalityItemsUsed) {
        this.vitalityItemsUsed = vitalityItemsUsed;
    }

    public void setAbilityPointsMainClassUsed(int abilityPointsMainClassUsed) {
        this.abilityPointsMainClassUsed = abilityPointsMainClassUsed;
    }

    public void setAbilityPointsDualClassUsed(int abilityPointsDualClassUsed) {
        this.abilityPointsDualClassUsed = abilityPointsDualClassUsed;
    }

    public void setRevelationSkillMainClass1(int revelationSkillMainClass1) {
        this.revelationSkillMainClass1 = revelationSkillMainClass1;
    }

    public void setRevelationSkillMainClass2(int revelationSkillMainClass2) {
        this.revelationSkillMainClass2 = revelationSkillMainClass2;
    }

    public void setRevelationSkillDualClass1(int revelationSkillDualClass1) {
        this.revelationSkillDualClass1 = revelationSkillDualClass1;
    }

    public void setRevelationSkillDualClass2(int revelationSkillDualClass2) {
        this.revelationSkillDualClass2 = revelationSkillDualClass2;
    }

    public void setExtendDrop(String extendDrop) {
        this.extendDrop = extendDrop;
    }

    public void setFortuneTelling(int fortuneTelling) {
        this.fortuneTelling = fortuneTelling;
    }

    public void setFortuneTellingBlackCat(boolean fortuneTellingBlackCat) {
        this.fortuneTellingBlackCat = fortuneTellingBlackCat;
    }

    public void setHuntingZoneResetTime(String huntingZoneResetTime) {
        this.huntingZoneResetTime = huntingZoneResetTime;
    }

    public void setAutoCp(int autoCp) {
        this.autoCp = autoCp;
    }

    public void setAutoHp(int autoHp) {
        this.autoHp = autoHp;
    }

    public void setAutoMp(int autoMp) {
        this.autoMp = autoMp;
    }

    public void setExpOff(boolean expOff) {
        this.expOff = expOff;
    }

    public void setItemsRewarded(boolean itemsRewarded) {
        this.itemsRewarded = itemsRewarded;
    }

    public void setHenna1Duration(long hennaDuration) {
        this.henna1Duration = hennaDuration;
    }

    public void setHenna2Duration(long hennaDuration) {
        this.henna2Duration = hennaDuration;
    }

    public void setHenna3Duration(long hennaDuration) {
        this.henna3Duration = hennaDuration;
    }

    public void setVisualHairId(int visualHairId) {
        this.visualHairId = visualHairId;
    }

    public void setVisualHairColorId(int visualHairColorId) {
        this.visualHairColorId = visualHairColorId;
    }

    public void setVisualFaceId(int visualFaceId) {
        this.visualFaceId = visualFaceId;
    }

    public void setInstanceOrigin(String instanceOrigin) {
        this.instanceOrigin = instanceOrigin;
    }

    public void setInstanceRestore(int instanceRestore) {
        this.instanceRestore = instanceRestore;
    }

    public void setClaimedClanRewards(int claimedClanRewards) {
        this.claimedClanRewards = claimedClanRewards;
    }

    public void setCondOverrideKey(String condOverrideKey) {
        this.condOverrideKey = condOverrideKey;
    }

    public void setAttendanceDate(long attendanceDate) {
        this.attendanceDate = attendanceDate;
    }

    public void setAttendanceIndex(int attendanceIndex) {
        this.attendanceIndex = attendanceIndex;
    }

    public void setUnclaimedOlympiadPoints(int unclaimedOlympiadPoints) {
        this.unclaimedOlympiadPoints = unclaimedOlympiadPoints;
    }

    public void setMonsterReturn(int monsterReturn) {
        this.monsterReturn = monsterReturn;
    }

    public void setUiKeyMapping(String uiKeyMapping) {
        this.uiKeyMapping = uiKeyMapping;
    }

    public byte getRevengeTeleports() {
        return revengeTeleports;
    }

    public byte getRevengeLocations() {
        return revengeLocations;
    }

    public void useRevengeLocation() {
        revengeLocations--;
    }

    public void useRevengeTeleport() {
        revengeTeleports--;
    }

    public void resetRevengeData() {
        revengeTeleports = REVENGE_USABLE_FUNCTIONS;
        revengeLocations = REVENGE_USABLE_FUNCTIONS;
    }
}
