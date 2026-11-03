---
status: todo
attempts: 1
title: "Color text in terminal stdout of harness"
source: backlog.md
depends_on: nothing

## Task

This changes is scoped only to the generic harness in the ".harness-loop" folder. The harness is supposed to be repository agnostic so do not put things specific to this repo in the harness code. 
Currentle the harness outputs the output of the model in the terminal stdout, but it is all in the same colo and kinda blurs together.
It would be better if one could see at a glance what the model is currently doing, each type of action being distinguished by a specific color.
There will be a different color for each of thinking, answering, tool calling, reading prompts and other possible actions.
Analyze the situation and determine if this is something that can be achieved in the code of the harness-loop, or if this is something dependent from the shell it is run from, taking into account the harnes can be run in both Windows (Git Bash) and Macos (bash, zsh) shells.