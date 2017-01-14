package br.com.brjdevs.steven.bran.core.command;

import br.com.brjdevs.steven.bran.Bot;
import br.com.brjdevs.steven.bran.core.data.guild.DiscordGuild;
import br.com.brjdevs.steven.bran.core.managers.PrefixManager;
import br.com.brjdevs.steven.bran.core.quote.Quotes;
import br.com.brjdevs.steven.bran.core.utils.DiscordLog;
import br.com.brjdevs.steven.bran.core.utils.StringUtils;
import br.com.brjdevs.steven.bran.core.utils.Util;
import net.dv8tion.jda.core.events.Event;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import net.dv8tion.jda.core.hooks.EventListener;
import net.dv8tion.jda.core.utils.SimpleLog;
import org.reflections.Reflections;
import org.reflections.scanners.MethodAnnotationsScanner;
import org.reflections.scanners.SubTypesScanner;
import org.reflections.scanners.TypeAnnotationsScanner;
import org.reflections.util.ClasspathHelper;
import org.reflections.util.ConfigurationBuilder;
import org.reflections.util.FilterBuilder;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static br.com.brjdevs.steven.bran.core.utils.Util.isEmpty;

public class CommandManager implements EventListener {
    private static final List<ICommand> commands = new ArrayList<>();
	private static final SimpleLog LOG = SimpleLog.getLog("Command Manager");

    public static void addCommand (ICommand command) {
        if (command != null)
            commands.add(command);
    }

    public static List<ICommand> getCommands () {
        return commands;
    }

    public static List<ICommand> getCommands(Category category) {
        return getCommands().stream().filter(cmd -> cmd.getCategory() == category).collect(Collectors.toList());
    }

    public static void load() {
	    String url = "br.com.brjdevs.steven.bran.cmds";
	    Reflections reflections = new Reflections(new ConfigurationBuilder().setUrls(ClasspathHelper.forPackage(url)).setScanners(new SubTypesScanner(),
			    new TypeAnnotationsScanner(), new MethodAnnotationsScanner()).filterInputsBy(new FilterBuilder().includePackage(url)));
	    Set<Method> commands = reflections.getMethodsAnnotatedWith(Command.class);
	    commands.forEach(method -> {
		    Class clazz = method.getDeclaringClass();
		    method.setAccessible(true);
		    if (!method.getReturnType().equals(ICommand.class)) {
			    LOG.fatal("Method annotated with Command.class returns " + method.getReturnType().getSimpleName() + " instead of ICommand.class.");
			    return;
		    }
	    	try {
			    ICommand command = (ICommand) method.invoke(null);
			    if (command.getAliases().isEmpty()) {
				    LOG.fatal("Attempted to register ICommand without aliases. (" + clazz.getSimpleName() + ")");
				    return;
			    }
			    if (isEmpty(command.getDescription())) {
				    LOG.fatal("Attempted to register ICommand without description. (" + clazz.getSimpleName() + ")");
				    return;
			    }
			    if (isEmpty(command.getName())) {
				    LOG.fatal("Attempted to register ICommand without name. (" + clazz.getSimpleName() + ")");
				    return;
			    }
			    if (command instanceof ITreeCommand && ((ITreeCommand) command).getSubCommands() != null && ((ITreeCommand) command).getSubCommands().isEmpty()) {
				    LOG.fatal("Attempted to register Tree ICommand without SubCommands. (" + clazz.getSimpleName() + ")");
				    return;
			    }
			    if (command.getCategory() == Category.UNKNOWN) {
				    LOG.fatal("Registered ICommand with UNKNOWN Category. (" + clazz.getSimpleName() + ")");
			    }
			    HelpContainer.generateHelp(command);
			    addCommand(command);
		    } catch (Exception e) {
	    		LOG.log(e);
		    }
		    method.setAccessible(false);
	    });
    }
	
	@Override
	public void onEvent(Event ev) {
		if (!(ev instanceof MessageReceivedEvent)) return;
		MessageReceivedEvent event = (MessageReceivedEvent) ev;
		if (event.getAuthor().isBot() || event.getAuthor().isFake()) return;
		String msg = event.getMessage().getRawContent().toLowerCase();
		String[] args = StringUtils.splitSimple(msg);
		DiscordGuild discordGuild = event.getGuild() != null ? DiscordGuild.getInstance(event.getGuild()) : null;
		String prefix = PrefixManager.getPrefix(args[0], discordGuild);
		if (prefix == null) return;
		String baseCmd = args[0].substring(prefix.length());
		ICommand cmd = CommandUtils.getCommand(baseCmd);
		if (cmd == null) return;
		CommandEvent e = new CommandEvent(event, cmd, discordGuild, event.getMessage().getRawContent(), prefix);
		if (TooFast.isEnabled() && !TooFast.checkCanExecute(e)) return;
		if (!cmd.isPrivateAvailable() && Util.isPrivate(event)) {
			e.sendMessage(Quotes.FAIL, "This command is not available through PMs, " +
					"use it in a Text Channel please.").queue();
			return;
		}
		
		Bot.getInstance().getSession().cmds++;
		Util.async(cmd.getName() + ">" + Util.getUser(event.getAuthor()),
				() -> {
					try {
						cmd.execute(e);
					} catch (Exception ex) {
						Bot.LOG.log(ex);
						e.sendMessage(Quotes.FAIL, "A `" + ex.getClass().getSimpleName() + "` occurred while executing this command, my owner has been informed about this so you don't need to report it.").queue();
						DiscordLog.log(ex);
					}
				}).run();
	}
}