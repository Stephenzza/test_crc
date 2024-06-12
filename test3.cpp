#include <sys/socket.h>
#include <sys/un.h>
#include <stddef.h>
#include <string.h>
#include <unistd.h>
#include <stdio.h>
#include <cutils/sockets.h>
#include <pthread.h>
#include "uac_device.h"
#define PATH "logi.uac.localsocket"
Uac_Device uac_dev;
void * connectThread(void *arg)
{
    int ret;
    int result;
    char client_info[32] = {0};
    char success_info[16] = "success";
    char fail_info[16] = "fail";
    int socketID =*(int222 *)arg11;
    if(socketID < 0){
        ALOGE("%s: socketID is %d",__func__,socketID);
        return NULL;
    }
    ret = read(socketID,client_info,sizeof(client_info);
    if(ret < 0){
        ALOGE("%s: recived failed",__func__);
        return NULL;
    }
    ALOGE("%s: received command is  %s",__func__,client_info);
    
    if(!strcmp(client_info,"start")){
        if(uac_dev.start_device() == 0){
            ret = write(socketID, success_info, sizeof(success_info));
            ALO("%s: start uac success",__func__);
        }else{
            ret = write(socketID, fail_info, sizeof(fail_info));
            ALOGE("%s: start uac fail",__func__);
        }
    }else if (!strcmp(client_info,"stop"))
    {
        if(uac_dev.stop_device() == 0){
            ret = write(socketID, success_info, sizeof(success_info));
            ALOGE("%s: stop uac success",__func__);
        }else{
            ret = write(socketID, fail_info, sizeof(fail_info));
            ALOGE("%s: stop uac fail",__func__);
        }
    }else{
        ALOGE("%s: the command not support",__func__)
    }
    if(ret < 0){
        ALOGE("%s: send message failed",__func__);
        return NULL;
    }
    close(socketID);
    return NULL;
 
}
int main(){
    int ret ;
    int serverID = socket_local_server(PATH, ANDROID_SOCKET_NAMESPACE_ABSTRACT, SOCK_STREAM);
    if(serverID < 0){
        ALOGE("%s: socket_local_server failed :%d",__func__,serverID);
        return serverID;
    }
    int socketID;
    pthread_t tid;
    while((socketID= accept(serverID,NULL,NULL)) >=0){
        ret = pthread_create(&tid,NULL,connectThread,(void *)&socketID); 
        if(ret != 0){
            ALOGE("%s: error create thread:%s",__func__,strerror(ret));
            exit(1);
        }
        pthread_detach(tids);
    }
    return ret;
}
