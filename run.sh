#!/usr/bin/env bash

echo "Running Kraken in " "${PROFILE}"

if [ -z "${PROFILE}" ]; then
    echo "Run Configuration [Local, Test, Production] not specified. Exiting..."
    exit 1
elif [ "${DJANGO_CONFIGURATION}" = "Local" ]; then
    echo "Starting  in Local Mode"

    # Start in with no specific configuration
    java -jar Kraken.jar

elif [ "${PROFILE}" = "Debug" ]; then
    echo "Starting  in Debug Mode"

    java -Xdebug -Xrunjdwp:server=y,transport=dt_socket,suspend=n -jar  Kraken.jar

elif [ "${PROFILE}" = "Production" ]; then
    echo "- Setting up NGINX... "
    cp ./django_nginx.conf /etc/nginx/sites-available/
    ln -s /etc/nginx/sites-available/django_nginx.conf /etc/nginx/sites-enabled
    echo "daemon off;" >> /etc/nginx/nginx.conf
    echo "Done"
    echo "- Collecting static file..."
    python manage.py collectstatic --noinput  # collect static files
    echo "Done"

    # Prepare log files and start outputting logs to stdout
    #mkdir -p /srv/logs/ && touch /srv/logs/gunicorn.log
    #mkdir -p /srv/logs/ && touch /srv/logs/access.log
    #tail -n 0 -f /srv/logs/*.log &

    # Start Gunicorn processes
    echo "- Starting Gunicorn..."

    exec gunicorn amanda.wsgi:application \
    --name amanda-server \
    --bind unix:django_app.sock \
    --workers 3 \
    --log-level=ERROR &
    echo "Done"
    #--log-file=/srv/logs/gunicorn.log \