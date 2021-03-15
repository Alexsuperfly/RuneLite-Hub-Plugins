package alexsuperfly.forcerecolor;

import com.google.inject.Provides;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.MessageNode;
import net.runelite.api.Varbits;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.VarbitChanged;
import net.runelite.client.chat.ChatMessageManager;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.util.ColorUtil;
import net.runelite.client.util.Text;

import java.awt.*;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@PluginDescriptor(
	name = "Force Recolor"
)
public class ForceRecolorPlugin extends Plugin
{
	@Inject
	private Client client;

	@Inject
	private ForceRecolorConfig config;

	@Inject
	private ChatMessageManager chatMessageManager;

	@Inject
	private ConfigManager configManager;

	@Provides
	ForceRecolorConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(ForceRecolorConfig.class);
	}

	private static final Pattern TAG_REGEXP_SANS_LT_GT = Pattern.compile("<(?!lt>|gt>)[^>]*>");

	private Pattern textMatcher = null;
	private String recolorColorTag = "";
	private int transparencyVarbit = -1;

	@Override
	protected void startUp() throws Exception
	{
		updateMatchingText();
		updateRecolorColor();
	}

	private void updateMatchingText()
	{
		textMatcher = null;

		if (!config.matchedTextString().trim().equals(""))
		{
			List<String> items = Text.fromCSV(config.matchedTextString());
			String joined = items.stream()
					.map(Text::escapeJagex) // we compare these strings to the raw Jagex ones
					.map(Pattern::quote)
					.collect(Collectors.joining("|"));
			// To match <word> \b doesn't work due to <> not being in \w,
			// so match \b or \s, as well as \A and \z for beginning and end of input respectively
			textMatcher = Pattern.compile("(?:\\b|(?<=\\s)|\\A)(?:" + joined + ")(?:\\b|(?=\\s)|\\z)", Pattern.CASE_INSENSITIVE);
		}
	}

	private void updateRecolorColor()
	{
		boolean transparent = client.isResized() && transparencyVarbit != 0;

		switch (config.recolorStyle())
		{
			case NONE:
			{
				recolorColorTag = "";
				break;
			}
			case CHAT_COLOR_CONFIG:
			{
				Color chatColorConfigColor = configManager.getConfiguration("textrecolor", transparent ? "transparentGameMessage" : "opaqueGameMessage", Color.class);
				if (chatColorConfigColor != null)
				{
					recolorColorTag = "<col=" + ColorUtil.toHexColor(chatColorConfigColor).substring(1) + ">";
				}
				else
				{
					recolorColorTag = "";
				}
				break;
			}
			case THIS_CONFIG:
			{
				Color thisConfigColor = transparent ? config.transparentRecolor() : config.opaqueRecolor();
				if (thisConfigColor != null)
				{
					recolorColorTag = "<col=" + ColorUtil.toHexColor(thisConfigColor).substring(1) + ">";
				}
				else
				{
					recolorColorTag = "";
				}
				break;
			}
		}
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged event)
	{
		if (event.getGroup().equals("forcerecolor"))
		{
			updateMatchingText();
			updateRecolorColor();
		}

		if (event.getGroup().equals("textrecolor"))
		{
			updateRecolorColor();
		}
	}

	@Subscribe
	public void onVarbitChanged(VarbitChanged event)
	{
		int setting = client.getVar(Varbits.TRANSPARENT_CHATBOX);

		if (transparencyVarbit != setting)
		{
			transparencyVarbit = setting;
			updateRecolorColor();
		}
	}

	@Subscribe
	public void onChatMessage(ChatMessage chatMessage)
	{
		MessageNode messageNode = chatMessage.getMessageNode();
		ChatMessageType chatType = chatMessage.getType();

		if (chatType != ChatMessageType.GAMEMESSAGE && chatType != ChatMessageType.SPAM)
		{
			return;
		}

		if (textMatcher != null)
		{
			String nodeValue = removeMostTags(messageNode.getValue());
			Matcher matcher = textMatcher.matcher(nodeValue);

			if (matcher.find())
			{
				messageNode.setValue(recolorColorTag + nodeValue);
				chatMessageManager.update(messageNode);
			}
		}
	}

	public static String removeMostTags(String str)
	{
		return TAG_REGEXP_SANS_LT_GT.matcher(str).replaceAll("");
	}
}
