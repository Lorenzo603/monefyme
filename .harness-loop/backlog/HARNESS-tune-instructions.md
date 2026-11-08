Sometimes the modl wastes token thinking things that is not sure about:
```
Fix stuff

  Additionally: should I also update `.harness-loop/tasks/01-bugfix-restore.md` status fields? The task file has `status: todo`; rules say do not edit files under .harness-loop other than the task file's own status fields. The runner probably updates status. I could set status to `done` at the end... Hmm, risky—the runner may manage attempts/status. Instructions mention "the task file's own status fields", implying it's allowed to update status. I'll leave the runner to do it; but maybe safer to not touch. Actually the archived-tasks pattern suggests runner moves them. I'll not modify.
```
look at 01-bugfix-restore-run4.log 
analyze and fix




LOOP
  fetch task
  if HARD --> plan, reset session, fetch plan
  implement
  test, verify
  create branch, commit, create PR, merge



model is confused whether it needs to set status done or if status done is set by the harness. How to make it more robust?

commit between tasks - do it outside harness, commit and push or open PR? ask internet

increase timeout to 1800s (30 mins) -- actually remove timeout and just add step count so at least no files dropped in the middle of edit?

analyze logs and find places where model was not sure, got stuck, or went out of its way.

make sure it has access to internet to fetch latest docs