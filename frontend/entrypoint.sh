#!/bin/sh
cat << EOF > /usr/share/nginx/html/config.json
{
    "BANKING_SERVICE_URL": "$BANKING_SERVICE_URL",
    "ACCOUNT_SERVICE_URL": "$ACCOUNT_SERVICE_URL",
    "CARD_SERVICE_URL": "$CARD_SERVICE_URL"
}
EOF
nginx -g "daemon off;"