# scala-swarm
This is a swarm simulation for Scala.js. It displays a swarm of bees chasing a wasp.

The code is based on Jeff Butterworth's [xswarm](http://ftp.funet.fi/pub/linux/util/X11/games/xswarm.tar.gz) for X11. It only uses integer arithmetic and performs well even with large numbers of bees.

## Usage
First, install [Seed](https://github.com/tindzk/seed).

Then, run the following commands:

```shell
seed bloop --tmpfs
bloop link swarm
cp index.html /tmp/build-scala-swarm/bloop
```

And navigate your browser to `/tmp/build-scala-swarm/bloop/index.html`.

## Licence
scala-swarm is licensed under the terms of the Apache v2.0 licence.
