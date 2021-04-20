# Force Recolor
Force recolors all game messages that contain specified text.

For example writing `divine` in the config will strip and replace the red color from the messages in chat telling you they are about to wear off.  
Or `exchange` to change that awful green color that can barely be read with the transparent chatbox.

###Groups

As of version 1.1 color groups are available.  Groups are denoted by the delimiter character `::` (two colons) followed by a number from 0-9.
for example `exchange::1` would recolor all messages containing the word "exchange" to the color you have specified for group 1.
If a message were to be matched by multiple strings of different groups, whichever one is the lowest numbered group will be the color selected to recolor it to.
The default group is 0, so you do not have to explicitly declare one if you dont want to, any invalid groups will also be treated as group 0.
