# iDoneThis API, command line client

**This is a legacy tool. It was suing the iDoneThis v1 Api but this one has been replaced without backward compatbility**

A simple iDoneThis command line client that can list and post dones.

The client is written in Scala and serves as a quick example on how one could use the iDoneThis API. It is not packaged
as distributable. You may compile the sources to use it.


## Usage

On first usage the client will ask for your DoneThis API token.

### list [from *team-name*]
List your dones for a given team. When omitted the default team is used (see default team).
> $ list from my-team
>
> // Display dones from team 'my-team'

> $ list
>
> // Display dones from your default team

### add [in *team-name*] *done description*
Add a done into the given team. When the team is omitted the default team is used (see default team).
> $ add in my-team Playing with iDoneThis API
> 
> // Add the done 'Playing with iDoneThis API' into the team 'my-team'

> $ add Playing with iDoneThis API
>
> // Add the done 'Playing with iDoneThis API' into your default team.

## Default team
At this time, the default team is the first one where 'is_personal' is true
