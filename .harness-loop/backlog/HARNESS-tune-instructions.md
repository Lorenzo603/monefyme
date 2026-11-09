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
  commit


analyze logs and find places where model was not sure, got stuck, or went out of its way.
--> get new logs

make sure it has access to internet to fetch latest docs
