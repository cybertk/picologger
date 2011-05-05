objects = agentd.o fdevent.o util.o services.o

TARBALL = agentd.tar.gz
#CFLAGS += -DDEBUG=1

agentd: $(objects)
	$(CC) -o agentd $(objects) 

pack:
	tar czf $(TARBALL) *
clean:
	rm $(objects) agentd
