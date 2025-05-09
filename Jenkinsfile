pipeline {
    agent any
    
    stages {
        stage('部署manager-api') {
            steps {
                echo '部署manager-api服务 (端口8002)'
                sh '''
                    # 检查服务状态并停止
                    if sudo systemctl is-active --quiet xiaozhi-api; then
                        echo "停止现有xiaozhi-api服务..."
                        sudo systemctl stop xiaozhi-api
                    fi
                    
                    # 编译并部署
                    cd main/manager-api
                    mvn clean install -DskipTests
                    sudo systemctl start xiaozhi-api
                    
                    # 验证服务是否启动成功
                    sleep 5  # 等待服务启动
                    if ! sudo systemctl is-active --quiet xiaozhi-api; then
                        echo "ERROR: manager-api启动失败！"
                        exit 1
                    fi
                '''
            }
        }
        
        stage('部署manager-web') {
            steps {
                echo '部署manager-web服务 (端口8001)'
                sh '''
                    # 检查服务状态并停止
                    if sudo systemctl is-active --quiet xiaozhi-web; then
                        echo "停止现有xiaozhi-web服务..."
                        sudo systemctl stop xiaozhi-web
                    fi
                    
                    # 部署并启动
                    sudo systemctl start xiaozhi-web
                    
                    # 验证服务是否启动成功
                    sleep 5
                    if ! sudo systemctl is-active --quiet xiaozhi-web; then
                        echo "ERROR: manager-web启动失败！"
                        exit 1
                    fi
                '''
            }
        }
        
        stage('部署xiaozhi-server') {
            steps {
                echo '部署xiaozhi-server服务 (端口8000)'
                sh '''#!/bin/bash
                    # 检查端口是否被占用（假设xiaozhi-server使用8000端口）
                    if lsof -Pi :8000 -sTCP:LISTEN -t >/dev/null ; then
                        echo "停止现有xiaozhi-server服务..."
                        pkill -f "python -u app.py" || true
                        sleep 2
                    fi
                    
                    # 部署并启动

                    cd main/xiaozhi-server

                    # 确保tmp目录存在并设置权限
                    mkdir -p ./tmp
                    sudo chmod 775 ./tmp
                    
                    source /home/ubuntu/anaconda3/etc/profile.d/conda.sh
                    conda activate xiaozhi-esp32-server
                    nohup python -u app.py > ./tmp/server.log 2>&1 &
                    
                    # 验证服务是否启动成功
                    sleep 5
                    if ! lsof -Pi :8000 -sTCP:LISTEN -t >/dev/null ; then
                        echo "ERROR: xiaozhi-server启动失败！"
                        exit 1
                    fi
                '''
            }
        }
    }
    
    post {
        success {
            echo '所有服务部署成功！'
        }
        failure {
            echo '部署过程中出现错误！'
        }
    }
}
