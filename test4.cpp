#include <sys/socket.h>
#include <sys/un.h>
#include <stddef.h>
#include <string.h>
#include <unistd.h>
#include <stdio.h>
#include <time.h>
#include <cutils/sockets.h>
#define PATH "logi.uac.localsocket"
static int send_command_ctr_uac(char* com){
    ret;
    int socketID;
    char result[32] = {0};
    socketID = socket_local_client(PATH, ANDROID_SOCKET_NAMESPACE_ABSTRACT, SOCK_STREAM);
    if (socketID < 0)
    {
        printf("socketID is %d , locaksocket service not start\n",socketID);
        return socketID;
    }
    printf("command value is %s length is %d \n",command,strlen(command));
    ret = write(socketID, command, strlen(command));
    if(ret < 0){
        printf("send failed\n");
        return ret;
    }
    
    ret = read(socketID,result,sizeof(result));
    if(ret < 0){
        printf("recived failed ret is %d \n",ret);
        return ret;
    }else{
        printf("client recived from server: %s\n",result);
    }
 
    ret = close(socketID);
    if (ret < 0)
    {
        return ret;
    }
 
    return 0;
}
int main(int argc, char *argv[]) 
{
    send_command_ctr_uac("start");
    sleep(60);
    send_command_ctr_uac("stop");
    // sleep(5);
//     send_command_ctr_uac("xxxxxxxxxxxxx");
//     return 0;
}
