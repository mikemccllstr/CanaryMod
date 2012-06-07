package net.canarymod.api.entity;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.canarymod.Canary;
import net.canarymod.Colors;
import net.canarymod.Logman;
import net.canarymod.api.NetServerHandler;
import net.canarymod.api.Packet;
import net.canarymod.api.inventory.Inventory;
import net.canarymod.api.inventory.Item;
import net.canarymod.api.world.Dimension;
import net.canarymod.api.world.position.Location;
import net.canarymod.commands.CanaryCommand;
import net.canarymod.hook.command.PlayerCommandHook;
import net.canarymod.hook.player.ChatHook;
import net.canarymod.permissionsystem.PermissionProvider;
import net.canarymod.user.Group;
import net.minecraft.server.OEntityPlayerMP;

/**
 * Canary Player wrapper.
 * @author Chris
 *
 */
public class CanaryPlayer extends CanaryEntityLiving implements Player {
    private Pattern badChatPattern = Pattern.compile("[\u00a7\u2302\u00D7\u00AA\u00BA\u00AE\u00AC\u00BD\u00BC\u00A1\u00AB\u00BB]");
    private Group group; 
    private PermissionProvider permissions;
    private String prefix = null;
    private boolean muted;
    private String[] allowedIPs;
    
    public CanaryPlayer(OEntityPlayerMP entity) {
        super(entity);
        String[] data = Canary.usersAndGroups().getPlayerData(getName());
        group = Canary.usersAndGroups().getGroup(data[1]); 
        permissions = Canary.permissionManager().getPlayerProvider(getName());
        
        if(data[0] != null && (!data[0].isEmpty() && !data[0].equals(" "))) {
            prefix = data[0];
        }
        
        if(data[2] != null && !data[2].isEmpty()) {
            allowedIPs = data[2].split(",");
        }
    }

    /**
     * CanaryMod: Get player handle
     */
    @Override
    public OEntityPlayerMP getHandle() {
        return (OEntityPlayerMP) entity;
    }
    @Override
    public String getName() {
    	return ((OEntityPlayerMP)entity).v;
    }
    
    @Override
    public void chat(String message) {
        if (message.length() > 100) {
            kick("Message too long!");
        } 
        message = message.trim();
        Matcher m = badChatPattern.matcher(message);
        String out = message;
        
        if (m.find() && !this.canIgnoreRestrictions()) {
            out = message.replaceAll(m.group(), "");
        }
        message = out;
        
        //TODO: Add configuration for spam protection?
        
        if(message.startsWith("/")) {
            executeCommand(message.split(" "));
        }
        else {
            if(isMuted()) {
                notify("You are currently muted!");
            }
            else {
                String prefix = "<" + getColor() + getName() + Colors.White + "> ";
                ArrayList<Player> receivers = (ArrayList<Player>) Canary.getServer().getPlayerList();
                ChatHook hook = (ChatHook) Canary.hooks().callCancelableHook(new ChatHook(this, prefix, message, receivers));
                if(hook.isCancelled()) {
                    return;
                }
                receivers = hook.getReceiverList();
                for(Player player : receivers) {
                    if (hook.getPrefix().length() + hook.getMessage().length() >= 100) {
                        player.sendMessage(hook.getPrefix());
                        player.sendMessage(hook.getMessage().toString());
                    } else {
                        player.sendMessage(hook.getPrefix()+hook.getMessage().toString());
                    }
                }
            }
        }

    }

    @Override
    public void sendMessage(String message) {
        getNetServerHandler().sendMessage(message);
    }

    @Override
    public void addExhaustion(float exhaustion) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void removeExhaustion(float exhaustion) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public float getExhaustionLevel() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public void setHunger(int hunger) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public int getHunger() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public void addExperience(int experience) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void removeExperience(int experience) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public int getExperience() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public boolean isSleeping() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public void setSleeping(boolean sleeping) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void destroyItemHeld() {
        // TODO Auto-generated method stub
        
    }

    @Override
    public Item getItemHeld() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void dropItem(Item item) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public Location getSpawnPosition() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void setSpawnPosition(Location spawn) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public String getIP() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean executeCommand(String[] command) {
        try {
            String cmd = command[0];
            if (cmd.startsWith("/#") && hasPermission("canary.commands.vanilla."+cmd.replace("/#", ""))) {
                Canary.getServer().consoleCommand(Canary.glueString(command, 0, " ").replace("/#", ""), this);
                return true;
            }
            
            PlayerCommandHook hook = (PlayerCommandHook) Canary.hooks().callCancelableHook(new PlayerCommandHook(this, command));
            if (hook.isCancelled()) {
                return true;
            } // No need to go on, commands were parsed.
            
            //Check for canary permissions
            
            CanaryCommand toExecute = CanaryCommand.fromString(cmd.replace("/", ""));
            if(toExecute == null) {
                sendMessage(Colors.Rose + "Unknown command!");
                return false;
            }
            else {
                if(!toExecute.execute(this, command)) {
                    sendMessage(Colors.Rose + "Permission denied!");
                    return false;
                }
                return true;
            }
            
        } catch (Throwable ex) {
            Logman.logStackTrace("Exception in command handler: ", ex);
            if (isAdmin()) {
                sendMessage(Colors.Rose + "Exception occured. "+ex.getMessage());
            }
            return false;
        }
    }

    @Override
    public boolean canFly() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean isFlying() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public void setFlying(boolean flying) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void sendPacket(Packet packet) {
        getDimension().getEntityTracker().sendPacketToTrackedPlayer(this, packet);
    }

    @Override
    public Group getGroup() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Group[] getPlayerGroups() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void addToGroup(String group) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void addToGroup(Group group) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void removeFromGroup(String group) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void removeFromGroup(Group group) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public boolean hasPermission(String permission) {
        if(!group.hasPermission(permission)) {
            return permissions.queryPermission(permission);
        }
        return true;
    }

    @Override
    public boolean isAdmin() {
//        if(!group.isAdministratorGroup()) {
//            return permissions.queryPermission("canary.player.administrator");
//        }
        return true;
    }

    @Override
    public boolean canBuild() {
        if(!group.canBuild()) {
            return permissions.queryPermission("canary.world.build");
        }
        return true;
    }

    @Override
    public void setCanBuild(boolean canModify) {
        permissions.addPermission("canary.world.build", canModify, -1);
    }

    @Override
    public boolean canIgnoreRestrictions() {
        if(!group.canIgnorerestrictions()) {
            return permissions.queryPermission("canary.player.ignoreRestrictions");
        }
        return true;
    }

    @Override
    public void setCanIgnoreRestrictions(boolean canIgnore) {
        permissions.addPermission("canary.player.ignoreRestrictions", canIgnore, -1);
    }

    @Override
    public boolean isMuted() {
        return muted;
    }

    @Override
    public void setMuted(boolean muted) {
         this.muted = muted;
    }

    @Override
    public PermissionProvider getPermissionProvider() {
        return permissions;
    }

    @Override
    public Location getLocation() {
        return new Location(entity.bi.getCanaryDimension(), getX(), getY(), getZ(), getPitch(), getRotation());
    }

    @Override
    public Inventory getInventory() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void giveItem(Item item) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public boolean isInGroup(Group group, boolean includeChilds) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public void teleportTo(double x, double y, double z) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void teleportTo(double x, double y, double z, Dimension dim) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void teleportTo(double x, double y, double z, float pitch,
            float rotation, Dimension dim) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void teleportTo(double x, double y, double z, float pitch,
            float rotation) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void teleportTo(Location location) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void kick(String reason) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void notify(String message) {
        sendMessage(Colors.Rose+message);
        
    }

    @Override
    public String getColor() {
        if(prefix != null) {
            return Colors.Marker+prefix;
        }
        return Colors.Marker+group.prefix;
    }

    @Override
    public NetServerHandler getNetServerHandler() {
        return ((OEntityPlayerMP)entity).getServerHandler();
    }

    @Override
    public boolean isInGroup(String group, boolean parents) {
        if(parents) {
            ArrayList<Group> groups = this.group.parentsToList();
            for(Group g : groups) {
                if(g.name.equals(group)) {
                    return true;
                }
            }
        }
        else {
            if(this.group.name.equals(group)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public String[] getAllowedIPs() {
        return allowedIPs;
    }

}
