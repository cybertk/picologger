objects = agentd.o fdevent.o util.o commands.o client.o

TARBALL = agentd.tar.gz
CFLAGS += -DDEBUG=0 -g

agentd: $(objects)
	$(CC) $(CFLAGS) -o agentd $(objects) 

pack:
	tar czf $(TARBALL) *
clean:
	rm $(objects) agentd
