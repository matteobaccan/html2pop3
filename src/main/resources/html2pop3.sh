#! /bin/bash
# Autore Originale: vito <malco@tim.it>
# Avvia, riavvia, ferma e controlla html2pop3
# Successive modifiche: flevour <html2pop3@flevour.net>
# Changelog:
# 18/06/04 - il processo html2pop3 viene ora terminato con un kill del pid e non con un killall di ogni java


# Modificare con il percorso assoluto dell'interprete java
JAVA=/usr/bin/java

# Modificare con il percorso assoluto di html2pop3
HTML2POP3=/root/html2pop3/

# paramentri passati all'interprete java
COMANDO=" -Djava.net.preferIPv4Stack=true -Dsun.jnu.encoding=Cp1252 -Dfile.encoding=Cp1252 -cp html2pop3.jar it.baccan.html2pop3.HTML2POP3"


case "$1" in
    start)
	check=`pgrep -f -- "$COMANDO"`
	if [ "$check" = "" ]; then
		cd $HTML2POP3
		echo -n "Avvio di html2pop3..."
		echo
		# html2pop3 in background...
		$JAVA $COMANDO&
	else
		echo -n "html2pop3 e' gia' in esecuzione"
		echo
	fi
	;;
    stop)
	echo -n "Chiusura di html2pop3"
	echo
	## Termina html2pop3 con killall attendendo l'avvenuta chiusura
	pid=`pgrep -f -- "$COMANDO"`
	kill -9 $pid
	;;
    status)
	echo -n "Controllo lo stato di html2pop3: "
	echo
	## Controlla lo stato html2pop3
	ps -p 1 `pgrep -f -- "$COMANDO"` |
		 awk '{ORS="" ; if ($3 ~ /^[TWXZ]/) {print "1"} else print "0"}' |
		  awk '{if ($1 > 0) {print "Il processo ha qualche problema oppure e` inattivo"} else {print "Il processo e` attivo"} }'
	;;
    restart)
	echo -n "Chiusura di html2pop3"
	echo
	## Termina html2pop3 con killall attendendo l'avvenuta chiusura
	pid=`pgrep -f -- "$COMANDO"`
        kill -9 $pid

	cd $HTML2POP3
	echo -n "Riavvio di html2pop3.."
	echo
	# html2pop3 in background....
	$JAVA $COMANDO&
	;;
    *)
	echo "Sintassi: $0 {start|stop|status|restart}"
	exit 1
	;;
esac


