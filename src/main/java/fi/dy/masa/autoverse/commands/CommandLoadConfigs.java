package fi.dy.masa.autoverse.commands;

import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.server.MinecraftServer;
import fi.dy.masa.autoverse.config.Configs;

public class CommandLoadConfigs extends CommandBase
{
    @Override
    public String getName()
    {
        return "autoverse-loadconfigs";
    }

    @Override
    public String getUsage(ICommandSender sender)
    {
        return "/" + this.getName();
    }

    @Override
    public int getRequiredPermissionLevel()
    {
        return 2;
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException
    {
        Configs.loadConfigsFromFile();
        notifyCommandListener(sender, this, "autoverse.commands.loadconfig.success", new Object[0]);
    }
}
