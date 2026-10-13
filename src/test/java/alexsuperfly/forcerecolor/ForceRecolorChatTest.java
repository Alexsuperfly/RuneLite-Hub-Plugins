package alexsuperfly.forcerecolor;

import java.awt.Color;
import java.lang.reflect.Field;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.IterableHashTable;
import net.runelite.api.MessageNode;
import net.runelite.api.events.ScriptCallbackEvent;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.events.ConfigChanged;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

public class ForceRecolorChatTest
{
	private static final String COLOR = "<col=123456>";
	private Client client;
	private ClientThread clientThread;
	private MessageNode node;
	private ForceRecolorConfig config;
	private ConfigManager configManager;
	private ForceRecolorPlugin plugin;
	private Object[] stack;

	@Before
	public void setUp() throws Exception
	{
		client = mock(Client.class);
		clientThread = mock(ClientThread.class);
		node = mock(MessageNode.class);
		config = mock(ForceRecolorConfig.class);
		configManager = mock(ConfigManager.class);
		plugin = new ForceRecolorPlugin();
		inject("client", client);
		inject("clientThread", clientThread);
		inject("config", config);
		inject("configManager", configManager);

		IterableHashTable<MessageNode> messages = mock(IterableHashTable.class);
		when(messages.get(42L)).thenReturn(node);
		when(client.getMessages()).thenReturn(messages);
		when(client.getIntStack()).thenReturn(new int[]{0, 42});
		when(client.getIntStackSize()).thenReturn(2);
		when(client.macroExpand(anyString())).thenAnswer(call -> call.getArgument(0));
		when(node.getType()).thenReturn(ChatMessageType.GAMEMESSAGE);
		when(config.allMessageTypes()).thenReturn(true);
		when(config.recolorStyle()).thenReturn(RecolorStyle.THIS_CONFIG);
		when(config.opaqueRecolor()).thenReturn(new Color(0x123456));
		when(config.matchedTextString()).thenReturn("completed");
		doAnswer(call -> { ((Runnable) call.getArgument(0)).run(); return null; }).when(clientThread).invokeLater(any(Runnable.class));
		plugin.startUp();
	}

	@Test
	public void startupDefersGameAccessUntilClientThread() throws Exception
	{
		reset(clientThread);
		clearInvocations(client);
		plugin.startUp();
		verifyNoInteractions(client);
		org.mockito.ArgumentCaptor<Runnable> pending = org.mockito.ArgumentCaptor.forClass(Runnable.class);
		verify(clientThread).invokeLater(pending.capture());
		pending.getValue().run();
		verify(client).getVarbitValue(net.runelite.api.Varbits.TRANSPARENT_CHATBOX);
		verify(client).refreshChat();
	}
	private void inject(String name, Object value) throws Exception
	{
		Field field = ForceRecolorPlugin.class.getDeclaredField(name);
		field.setAccessible(true);
		field.set(plugin, value);
	}

	private void draw(String text)
	{
		stack = new Object[]{"channel", "username", text, "timestamp"};
		when(client.getObjectStack()).thenReturn(stack);
		when(client.getObjectStackSize()).thenReturn(stack.length);
		ScriptCallbackEvent event = new ScriptCallbackEvent();
		event.setEventName("chatMessageBuilding");
		plugin.onScriptCallbackEvent(event);
		assertEquals("channel", stack[0]);
		assertEquals("username", stack[1]);
		assertEquals("timestamp", stack[3]);
		verify(node, never()).setValue(anyString());
		verify(node, never()).setRuneLiteFormatMessage(anyString());
	}

	private void configure(String matches)
	{
		when(config.matchedTextString()).thenReturn(matches);
		ConfigChanged event = new ConfigChanged();
		event.setGroup("forcerecolor");
		plugin.onConfigChanged(event);
	}

	@Test
	public void questDefinitionAndReferenceSurvive()
	{
		String message = "<img=41> <str_quest_name_0=Darkness of Hallowvale>SirDuckens has completed a quest: <str_quest_name_0>";
		draw(message);
		assertEquals(COLOR + message, stack[2]);
	}

	@Test
	public void expandedQuestNameSurvives()
	{
		String expanded = "<img=41> SirDuckens has completed a quest: Darkness of Hallowvale";
		String raw = "<img=41> <str_quest_name_0=Darkness of Hallowvale>SirDuckens has completed a quest: <str_quest_name_0>";
		when(client.macroExpand(raw)).thenReturn(expanded);
		configure("Darkness of Hallowvale");
		draw(raw);
		assertEquals(COLOR + expanded, stack[2]);
	}

	@Test
	public void combatTaskWithIconRetainsMetadataBeforeColor()
	{
		String body = "Chief\u00a0Tains has completed a grandmaster combat task: Colosseum Grand Champion.";
		draw("<img=41> CA_ID:539|" + body);
		assertEquals("<img=41> CA_ID:539|" + COLOR + body, stack[2]);
	}

	@Test
	public void combatTaskWithoutIconRetainsDelimiter()
	{
		draw("CA_ID:539|Task completed.");
		assertEquals("CA_ID:539|" + COLOR + "Task completed.", stack[2]);
	}

	@Test
	public void existingChatColorsDoNotMoveTaskMetadata()
	{
		draw("<col=abcdef><img=41> CA_ID:539|Task <col=ff0000>completed</col>.</col>");
		assertEquals("<img=41> CA_ID:539|" + COLOR + "Task completed.", stack[2]);
	}

	@Test
	public void onlyColorsAreRemovedAndLineBreaksReapplyColor()
	{
		draw("<col=ff0000>completed</col><br><u>quest</u> <img=41> <lt> <gt> <str_future=feature>");
		assertEquals(COLOR + "completed<br>" + COLOR + "<u>quest</u> <img=41> <lt> <gt> <str_future=feature>", stack[2]);
	}

	@Test
	public void macroColorsAreExpandedBeforeRecoloring()
	{
		when(client.macroExpand("@red@completed")).thenReturn("<col=ff0000>completed");
		draw("@red@completed");
		assertEquals(COLOR + "completed", stack[2]);
	}

	@Test
	public void groupIronmanDelimiterStaysFirst()
	{
		when(node.getType()).thenReturn(ChatMessageType.CLAN_GIM_MESSAGE);
		draw("|<img=41> Task completed.");
		assertEquals("|" + COLOR + "<img=41> Task completed.", stack[2]);
	}

	@Test
	public void incompleteTaskMetadataDoesNotThrow()
	{
		draw("CA_ID:539 Task completed.");
		assertEquals(COLOR + "CA_ID:539 Task completed.", stack[2]);
	}

	@Test
	public void unmatchedMessagesRemainExactlyAsSupplied()
	{
		String original = "<col=ff0000><img=41> Nothing to match.</col>";
		draw(original);
		assertEquals(original, stack[2]);
	}

	@Test
	public void allMessageTypesSettingStillFiltersClanMessages()
	{
		when(config.allMessageTypes()).thenReturn(false);
		when(node.getType()).thenReturn(ChatMessageType.CLAN_MESSAGE);
		draw("Quest completed.");
		assertEquals("Quest completed.", stack[2]);
	}

	@Test
	public void lowestMatchingGroupWins()
	{
		when(configManager.getConfiguration("forcerecolor", "opaqueRecolorGroup1", Color.class)).thenReturn(Color.RED);
		when(configManager.getConfiguration("forcerecolor", "opaqueRecolorGroup9", Color.class)).thenReturn(Color.BLUE);
		configure("completed::9,completed::1");
		draw("Quest completed.");
		assertEquals("<col=ff0000>Quest completed.", stack[2]);
	}

	@Test
	public void redrawUsesNewConfigurationWithoutChangingStoredMessage()
	{
		draw("Quest completed.");
		assertEquals(COLOR + "Quest completed.", stack[2]);
		configure("unrelated");
		draw("Quest completed.");
		assertEquals("Quest completed.", stack[2]);
	}

	@Test
	public void unrelatedCallbacksDoNotReadScriptStacks()
	{
		ScriptCallbackEvent event = new ScriptCallbackEvent();
		event.setEventName("chatFilterCheck");
		plugin.onScriptCallbackEvent(event);
		verify(client, never()).getIntStack();
	}

	@Test
	public void noColorStyleStillPreservesSemanticTags()
	{
		when(config.recolorStyle()).thenReturn(RecolorStyle.NONE);
		configure("completed");
		draw("<col=ff0000>completed</col> <str_quest_name_0> <img=41>");
		assertEquals("completed <str_quest_name_0> <img=41>", stack[2]);
	}
}
